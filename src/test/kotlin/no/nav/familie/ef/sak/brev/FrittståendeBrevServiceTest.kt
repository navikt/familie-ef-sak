package no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.brev.dto.FrittståendeSanitybrevDto
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as KontrakterFrittståendeBrevDto

internal class FrittståendeBrevServiceTest {
    private val brevClient = mockk<BrevClient>()
    private val fagsakService = mockk<FagsakService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val iverksettClient = mockk<IverksettClient>()
    private val brevsignaturService = mockk<BrevsignaturService>()
    private val mellomlagringBrevService = mockk<MellomlagringBrevService>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()
    private val brevmottakereService = mockk<BrevmottakereService>()

    private val frittståendeBrevService =
        FrittståendeBrevService(
            brevClient,
            fagsakService,
            arbeidsfordelingService,
            iverksettClient,
            brevsignaturService,
            mellomlagringBrevService,
            familieDokumentClient,
            brevmottakereService,
        )
    private val fagsak = fagsak(fagsakpersoner(identer = setOf("01010172272")))

    private val frittståendeSanitybrevDto =
        FrittståendeSanitybrevDto(
            "123".toByteArray(),
            mottakere =
                BrevmottakereDto(
                    personer = listOf(BrevmottakerPerson("mottakerIdent", "navn", MottakerRolle.BRUKER)),
                    organisasjoner = emptyList(),
                ),
            tittel = "tittel",
        )

    private val frittståendeBrevSlot = slot<KontrakterFrittståendeBrevDto>()

    @BeforeEach
    internal fun setUp() {
        BrukerContextUtil.mockBrukerContext("Saksbehandler")
        frittståendeBrevSlot.clear()
        mockAvhengigheter()
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class Mottakere {
        private val organisasjon = BrevmottakerOrganisasjon("org1", "navn", MottakerRolle.FULLMAKT)
        private val person = BrevmottakerPerson("ident", "navn", MottakerRolle.BRUKER)

        @Test
        internal fun `Sanitybrev - skal kaste feil dersom både personer og organisasjoner i mottakere i dto er tomme lister`() {
            val feil =
                assertThrows<ApiFeil> {
                    frittståendeBrevService.sendFrittståendeSanitybrev(
                        fagsak.id,
                        frittståendeSanitybrevDto.copy(
                            mottakere = BrevmottakereDto(emptyList(), emptyList()),
                        ),
                    )
                }
            assertThat(feil.message).contains("Kan ikke sende frittstående brev uten at minst en brevmottaker er lagt til")
        }

        @Test
        internal fun `Sanitybrev - skal sette mottakere hvis personer finnes`() {
            frittståendeBrevService.sendFrittståendeSanitybrev(
                fagsak.id,
                frittståendeSanitybrevDto.copy(mottakere = BrevmottakereDto(listOf(person), emptyList())),
            )
            val mottakere = frittståendeBrevSlot.captured.mottakere!!
            assertThat(mottakere).hasSize(1)
            assertThat(mottakere[0].ident).isEqualTo(person.personIdent)
            assertThat(mottakere[0].navn).isEqualTo(person.navn)
            assertThat(mottakere[0].mottakerRolle).isEqualTo(person.mottakerRolle.tilIverksettDto())
            verify { brevmottakereService.slettBrevmottakereForFagsakOgSaksbehandlerHvisFinnes(fagsak.id, any()) }
        }

        @Test
        internal fun `Sanitybrev - skal sette mottakere hvis organisasjoner finnes`() {
            frittståendeBrevService.sendFrittståendeSanitybrev(
                fagsak.id,
                frittståendeSanitybrevDto.copy(mottakere = BrevmottakereDto(emptyList(), listOf(organisasjon))),
            )
            val mottakere = frittståendeBrevSlot.captured.mottakere!!
            assertThat(mottakere).hasSize(1)
            assertThat(mottakere[0].ident).isEqualTo(organisasjon.organisasjonsnummer)
            assertThat(mottakere[0].navn).isEqualTo(organisasjon.navnHosOrganisasjon)
            assertThat(mottakere[0].mottakerRolle).isEqualTo(organisasjon.mottakerRolle.tilIverksettDto())
            verify { brevmottakereService.slettBrevmottakereForFagsakOgSaksbehandlerHvisFinnes(fagsak.id, any()) }
        }

        @Test
        internal fun `Sanitybrev - skal sette mottakere hvis organisasjoner og personer finnes`() {
            frittståendeBrevService.sendFrittståendeSanitybrev(
                fagsak.id,
                frittståendeSanitybrevDto.copy(mottakere = BrevmottakereDto(listOf(person), listOf(organisasjon))),
            )

            val mottakere = frittståendeBrevSlot.captured.mottakere!!
            assertThat(mottakere).hasSize(2)
            assertThat(mottakere[0].ident).isEqualTo(person.personIdent)
            assertThat(mottakere[0].navn).isEqualTo(person.navn)
            assertThat(mottakere[0].mottakerRolle).isEqualTo(person.mottakerRolle.tilIverksettDto())

            assertThat(mottakere[1].ident).isEqualTo(organisasjon.organisasjonsnummer)
            assertThat(mottakere[1].navn).isEqualTo(organisasjon.navnHosOrganisasjon)
            assertThat(mottakere[1].mottakerRolle).isEqualTo(organisasjon.mottakerRolle.tilIverksettDto())
            verify { brevmottakereService.slettBrevmottakereForFagsakOgSaksbehandlerHvisFinnes(fagsak.id, any()) }
        }
    }

    private fun mockAvhengigheter() {
        every { fagsakService.hentAktivIdent(any()) } returns fagsak.hentAktivIdent()
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.hentEksternId(any()) } returns Long.MAX_VALUE
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns "123"
        every { brevsignaturService.lagSignaturMedEnhet(any<Fagsak>(), false) } returns
            SignaturDto(
                "Navn Navnesen",
                "En enhet",
                false,
            )
        justRun { iverksettClient.sendFrittståendeBrev(capture(frittståendeBrevSlot)) }
        justRun { mellomlagringBrevService.slettMellomlagretFrittståendeBrev(any(), any()) }
        justRun { brevmottakereService.slettBrevmottakereForFagsakOgSaksbehandlerHvisFinnes(any(), any()) }
    }

    companion object {
        @AfterAll
        @JvmStatic
        fun tearDown() {
            BrukerContextUtil.clearBrukerContext()
        }
    }
}
