package no.nav.familie.ef.sak.klage

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.brev.BrevsignaturService.Companion.ENHET_NAY
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsaker
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakType
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

internal class KlageServiceTest {
    private val fagsakService = mockk<FagsakService>()

    private val fagsakPersonService = mockk<FagsakPersonService>()

    private val klageClient = mockk<KlageClient>()

    private val infotrygdService = mockk<InfotrygdService>()

    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    private val featureToggleService = mockk<FeatureToggleService>(relaxed = true)

    private val klageService =
        KlageService(
            fagsakService,
            fagsakPersonService,
            klageClient,
            infotrygdService,
            arbeidsfordelingService,
            featureToggleService,
        )

    private val eksternFagsakId = 11L
    private val eksternBehandlingId = 22L
    private val personIdent = "100"
    private val fagsakPerson = fagsakPerson(setOf(PersonIdent(personIdent)))
    val fagsak = fagsak(eksternId = eksternFagsakId, person = fagsakPerson)
    private val saksbehandling =
        saksbehandling(
            fagsak,
            behandling(eksternId = eksternBehandlingId),
        )

    private val opprettKlageSlot = slot<OpprettKlagebehandlingRequest>()

    @BeforeEach
    internal fun setUp() {
        opprettKlageSlot.clear()
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { fagsakService.hentAktivIdent(saksbehandling.fagsakId) } returns personIdent
        every { fagsakPersonService.hentPerson(any()) } returns fagsakPerson
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet(ENHET_NAY, "enhet")
        every { featureToggleService.isEnabled(any()) } returns true

        justRun { klageClient.opprettKlage(capture(opprettKlageSlot)) }
    }

    @Nested
    inner class OpprettKlage {
        @Test
        internal fun `skal mappe riktige verdier ved manuelt opprettet klage`() {
            klageService.validerOgOpprettKlage(fagsak, OpprettKlageDto(LocalDate.now(), true, Klagebehandlingsårsak.ORDINÆR))

            val request = opprettKlageSlot.captured

            assertThat(request.ident).isEqualTo(personIdent)
            assertThat(request.eksternFagsakId).isEqualTo(eksternFagsakId.toString())
            assertThat(request.fagsystem).isEqualTo(Fagsystem.EF)
            assertThat(request.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
            assertThat(request.klageMottatt).isEqualTo(LocalDate.now())
            assertThat(request.behandlendeEnhet).isEqualTo(ENHET_NAY)
            assertThat(request.klageGjelderTilbakekreving).isEqualTo(true)
        }
    }

    @Nested
    inner class HarÅpenKlage {
        @Test
        internal fun `har ikke noen åpen sak`() {
            every { infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()) } returns listOf()

            val åpneKlager = klageService.hentÅpneKlagerInfotrygd(fagsakPerson.id)

            assertThat(åpneKlager.stønadstyper).isEmpty()
        }

        @ParameterizedTest
        @EnumSource(StønadType::class)
        internal fun `har åpen sak for stønadstype`(stønadType: StønadType) {
            every { infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()) } returns
                listOf(
                    åpenSak(
                        stønadstype = stønadType,
                    ),
                )

            val åpneKlager = klageService.hentÅpneKlagerInfotrygd(fagsakPerson.id)

            assertThat(åpneKlager.stønadstyper).containsExactly(stønadType)
        }

        private fun åpenSak(stønadstype: StønadType = StønadType.OVERGANGSSTØNAD) =
            InfotrygdSak(
                "1",
                stønadType = stønadstype,
                resultat = InfotrygdSakResultat.ÅPEN_SAK,
                type = InfotrygdSakType.KLAGE,
            )
    }

    @Nested
    inner class Mapping {
        val eksternIdBT = 1L
        val eksternIdOS = 2L
        val eksternIdSP = 3L

        @Test
        internal fun `kaller på klage med eksternFagsakId`() {
            val eksternFagsakIdSlot = slot<Set<Long>>()
            val fagsaker = fagsaker()

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(capture(eksternFagsakIdSlot)) } returns emptyMap()

            klageService.hentBehandlinger(UUID.randomUUID())
            assertThat(eksternFagsakIdSlot.captured).containsExactlyInAnyOrder(
                eksternIdBT,
                eksternIdOS,
                eksternIdSP,
            )
        }

        @Test
        internal fun `skal mappe fagsakId til riktig stønadstype`() {
            val fagsaker = fagsaker()
            val klageBehandlingerDto = klageBehandlingerDto()

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns klageBehandlingerDto

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.overgangsstønad.single()).isEqualTo(klageBehandlingerDto[eksternIdOS]!!.single())
            assertThat(klager.barnetilsyn.single()).isEqualTo(klageBehandlingerDto[eksternIdBT]!!.single())
            assertThat(klager.skolepenger.single()).isEqualTo(klageBehandlingerDto[eksternIdSP]!!.single())
            assertThat(klager.skolepenger.single()).isNotEqualTo(klager.barnetilsyn.single())
        }

        @Test
        internal fun `skal returnere tomme lister dersom eksternFagsakId ikke eksisterer`() {
            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns Fagsaker(null, null, null)

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.overgangsstønad).isEmpty()
            assertThat(klager.barnetilsyn).isEmpty()
            assertThat(klager.skolepenger).isEmpty()
        }

        @Test
        internal fun `Hent klage - skal bruke vedtaksdato fra kabal hvis resultat IKKE_MEDHOLD og avsluttet i kabal`() {
            val fagsaker = fagsaker()

            val tidsPunktAvsluttetIKabal = LocalDateTime.of(2022, Month.OCTOBER, 1, 0, 0)
            val tidspunktAvsluttetIFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            val klagebehandlingAvsluttetKabal =
                klageBehandlingDto(
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat =
                        listOf(
                            KlageinstansResultatDto(
                                type = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
                                utfall = null,
                                mottattEllerAvsluttetTidspunkt = tidsPunktAvsluttetIKabal,
                                journalpostReferanser = listOf(),
                                årsakFeilregistrert = null,
                            ),
                        ),
                    vedtaksdato = tidspunktAvsluttetIFamilieKlage,
                )

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns
                mapOf(
                    eksternIdOS to listOf(klagebehandlingAvsluttetKabal),
                    eksternIdBT to emptyList(),
                    eksternIdSP to emptyList(),
                )

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.overgangsstønad.first().vedtaksdato).isEqualTo(tidsPunktAvsluttetIKabal)
        }

        @Test
        internal fun `Hent klage - hvis resultat fra kabal ikke foreligger enda skal vedtaksdato være null behandlingsresultat er IKKE_MEDHOLD`() {
            val fagsaker = fagsaker()
            val tidspunktAvsluttetFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            val klagebehandlingIkkeAvsluttetKabal =
                klageBehandlingDto(
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat = emptyList(),
                    vedtaksdato = tidspunktAvsluttetFamilieKlage,
                )

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns
                mapOf(
                    eksternIdOS to listOf(klagebehandlingIkkeAvsluttetKabal),
                    eksternIdBT to emptyList(),
                    eksternIdSP to emptyList(),
                )

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.overgangsstønad.first().vedtaksdato).isNull()
        }

        @Test
        internal fun `Hent klage - skal bruke vedtaksdato fra klageløsning dersom behandling ikke er oversendt kabal`() {
            val fagsaker = fagsaker()
            val tidspunktAvsluttetFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns
                mapOf(
                    eksternIdOS to
                        listOf(
                            klageBehandlingDto(
                                resultat = BehandlingResultat.MEDHOLD,
                                klageinstansResultat = emptyList(),
                                vedtaksdato = tidspunktAvsluttetFamilieKlage,
                            ),
                        ),
                    eksternIdBT to
                        listOf(
                            klageBehandlingDto(
                                resultat = BehandlingResultat.IKKE_MEDHOLD_FORMKRAV_AVVIST,
                                klageinstansResultat = emptyList(),
                                vedtaksdato = tidspunktAvsluttetFamilieKlage,
                            ),
                        ),
                    eksternIdSP to emptyList(),
                )

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.overgangsstønad.first().vedtaksdato).isEqualTo(tidspunktAvsluttetFamilieKlage)
            assertThat(klager.barnetilsyn.first().vedtaksdato).isEqualTo(tidspunktAvsluttetFamilieKlage)
        }

        @Test
        internal fun `Hent klage - skal bruke vedtaksdato fra kabal dersom behandlingen er feilregistrert i kabal`() {
            val fagsaker = fagsaker()

            val tidsPunktAvsluttetIKabal = LocalDateTime.of(2022, Month.OCTOBER, 1, 0, 0)
            val tidspunktAvsluttetIFamilieKlage = LocalDateTime.of(2022, Month.AUGUST, 1, 0, 0)

            val årsakFeilregistrert = "Klage registrert på feil vedtak"
            val klagebehandlingAvsluttetKabal =
                klageBehandlingDto(
                    resultat = BehandlingResultat.IKKE_MEDHOLD,
                    klageinstansResultat =
                        listOf(
                            KlageinstansResultatDto(
                                type = BehandlingEventType.BEHANDLING_FEILREGISTRERT,
                                utfall = null,
                                mottattEllerAvsluttetTidspunkt = tidsPunktAvsluttetIKabal,
                                journalpostReferanser = listOf(),
                                årsakFeilregistrert = årsakFeilregistrert,
                            ),
                        ),
                    vedtaksdato = tidspunktAvsluttetIFamilieKlage,
                )

            every { fagsakService.finnFagsakerForFagsakPersonId(any()) } returns fagsaker
            every { klageClient.hentKlagebehandlinger(any()) } returns
                mapOf(
                    eksternIdOS to listOf(klagebehandlingAvsluttetKabal),
                    eksternIdBT to emptyList(),
                    eksternIdSP to emptyList(),
                )

            val klager = klageService.hentBehandlinger(UUID.randomUUID())

            assertThat(klager.overgangsstønad.first().vedtaksdato).isEqualTo(tidsPunktAvsluttetIKabal)
            assertThat(
                klager.overgangsstønad
                    .first()
                    .klageinstansResultat
                    .first()
                    .årsakFeilregistrert,
            ).isEqualTo(årsakFeilregistrert)
        }

        private fun fagsaker() =
            Fagsaker(
                fagsak(stønadstype = StønadType.OVERGANGSSTØNAD, eksternId = eksternIdOS),
                fagsak(stønadstype = StønadType.BARNETILSYN, eksternId = eksternIdBT),
                fagsak(stønadstype = StønadType.SKOLEPENGER, eksternId = eksternIdSP),
            )

        private fun klageBehandlingDto(
            resultat: BehandlingResultat? = null,
            vedtaksdato: LocalDateTime? = null,
            klageinstansResultat: List<KlageinstansResultatDto> = emptyList(),
        ) = KlagebehandlingDto(
            id = UUID.randomUUID(),
            fagsakId = UUID.randomUUID(),
            status = BehandlingStatus.UTREDES,
            opprettet = LocalDateTime.now(),
            mottattDato = LocalDate.now().minusDays(1),
            resultat = resultat,
            årsak = null,
            vedtaksdato = vedtaksdato,
            klageinstansResultat = klageinstansResultat,
        )

        private fun klageBehandlingerDto() =
            mapOf(
                eksternIdOS to listOf(klageBehandlingDto()),
                eksternIdBT to listOf(klageBehandlingDto()),
                eksternIdSP to listOf(klageBehandlingDto()),
            )
    }

    @Nested
    inner class Validering {
        @Test
        internal fun `skal ikke kunne opprette klage med krav mottatt frem i tid`() {
            val opprettKlageDto = OpprettKlageDto(mottattDato = LocalDate.now().plusDays(1), false, Klagebehandlingsårsak.ORDINÆR)
            val feil = assertThrows<ApiFeil> { klageService.validerOgOpprettKlage(fagsak(), opprettKlageDto) }

            assertThat(feil.feil).contains("Kan ikke opprette klage med krav mottatt frem i tid for fagsak=")
        }

        @Test
        internal fun `skal ikke kunne opprette dersom enhetId ikke finnes`() {
            every { arbeidsfordelingService.hentNavEnhet(any()) } returns null

            val opprettKlageDto = OpprettKlageDto(mottattDato = LocalDate.now(), false, Klagebehandlingsårsak.ORDINÆR)
            val feil = assertThrows<ApiFeil> { klageService.validerOgOpprettKlage(fagsak, opprettKlageDto) }

            assertThat(feil.feil).isEqualTo("Finner ikke behandlende enhet for personen")
        }
    }
}
