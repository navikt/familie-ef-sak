package no.nav.familie.ef.sak.vedtak.uttrekk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker.ArbeidssøkerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker.ArbeidssøkerPeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker.ArbeidssøkerResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType.BARNET_ER_SYKT
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Adressebeskyttelse as DtoAdressebeskyttelse

internal class UttrekkArbeidssøkerServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakService: FagsakService
    @Autowired private lateinit var tilgangService: TilgangService
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var uttrekkArbeidssøkerRepository: UttrekkArbeidssøkerRepository
    @Autowired private lateinit var beregnYtelseSteg: BeregnYtelseSteg
    @Autowired private lateinit var rolleConfig: RolleConfig

    private lateinit var service: UttrekkArbeidssøkerService

    private val arbeidssøkerClient = mockk<ArbeidssøkerClient>()
    private val personService = mockk<PersonService>()

    private val fagsak = fagsak(fagsakpersoner(setOf("1")))
    private val behandling = behandling(fagsak)
    private val behandling2 = behandling(fagsak,
                                         type = BehandlingType.REVURDERING,
                                         forrigeBehandlingId = behandling.id,
                                         opprettetTid = behandling.sporbar.opprettetTid.plusDays(1))

    private val januar2021 = YearMonth.of(2021, 1)
    private val februar2021 = YearMonth.of(2021, 2)
    private val mars2021 = YearMonth.of(2021, 3)

    private val vedtaksperiode = opprettVedtaksperiode(januar2021, mars2021)
    private val vedtaksperiode2 = opprettVedtaksperiode(februar2021, februar2021,
                                                        aktivitetType = BARNET_ER_SYKT)
    private val vedtaksperiode3 = opprettVedtaksperiode(mars2021, mars2021,
                                                        aktivitetType = FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER)
    private val navn = Navn("fornavn", "", "", Metadata(false))

    @BeforeEach
    internal fun setUp() {
        every { arbeidssøkerClient.hentPerioder(any(), any(), any()) } returns ArbeidssøkerResponse(listOf())
        every { personService.hentPdlPersonKort(any()) } answers {
            firstArg<List<String>>().associateWith { lagPersonKort() }
        }
        service = UttrekkArbeidssøkerService(tilgangService,
                                             uttrekkArbeidssøkerRepository,
                                             fagsakService,
                                             personService,
                                             arbeidssøkerClient)
    }

    @Test
    internal fun `skal kjøre query uten problemer`() {
        assertThat(service.hentArbeidssøkereForUttrekk()).isEmpty()
    }

    @Test
    internal fun `skal ikke finne andre aktivitettyper enn de som søker etter arbeid`() {
        opprettdata()
        val arbeidssøkere = service.hentArbeidssøkereForUttrekk(februar2021)
        assertThat(arbeidssøkere).isEmpty()
    }

    @Test
    internal fun `behandlingIdForVedtak skal peke til behandlingen der vedtaket ble opprettet`() {
        opprettdata()

        val arbeidssøkereJan = service.hentArbeidssøkereForUttrekk(januar2021)
        assertThat(arbeidssøkereJan).hasSize(1)
        assertThat(arbeidssøkereJan[0].behandlingId).isEqualTo(behandling2.id)
        assertThat(arbeidssøkereJan[0].behandlingIdForVedtak).isEqualTo(behandling.id)

        val arbeidssøkereMars = service.hentArbeidssøkereForUttrekk(mars2021)
        assertThat(arbeidssøkereMars).hasSize(1)
        assertThat(arbeidssøkereMars[0].behandlingId).isEqualTo(behandling2.id)
        assertThat(arbeidssøkereMars[0].behandlingIdForVedtak).isEqualTo(behandling2.id)
    }

    @Test
    internal fun `hentUttrekkArbeidssøkere - finnes ikke noen arbeidssøkere valgt måned`() {
        opprettdata()
        service.opprettUttrekkArbeidssøkere(februar2021)
        assertThat(service.hentUttrekkArbeidssøkere(februar2021).arbeidssøkere).isEmpty()
    }

    @Test
    internal fun `hentUttrekkArbeidssøkere - skal opprette uttrekk for arbeidssøkere`() {
        opprettdata()

        service.opprettUttrekkArbeidssøkere(januar2021)

        val uttrekk = service.hentUttrekkArbeidssøkere(januar2021).arbeidssøkere
        assertThat(uttrekk).hasSize(1)
        assertThat(uttrekk[0].fagsakId).isEqualTo(behandling.fagsakId)
        assertThat(uttrekk[0].behandlingIdForVedtak).isEqualTo(behandling.id)
        assertThat(uttrekk[0].kontrollert).isFalse
        assertThat(uttrekk[0].registrertArbeidssøker).isFalse
    }

    @Test
    internal fun `hentUttrekkArbeidssøkere - vedtaket skal peke til endringen sin behandling`() {
        opprettdata()
        service.opprettUttrekkArbeidssøkere(mars2021)

        val uttrekk = service.hentUttrekkArbeidssøkere(mars2021).arbeidssøkere
        assertThat(uttrekk).hasSize(1)
        assertThat(uttrekk[0].fagsakId).isEqualTo(behandling.fagsakId)
        assertThat(uttrekk[0].behandlingIdForVedtak).isEqualTo(behandling2.id)
        assertThat(uttrekk[0].kontrollert).isFalse
    }

    @Test
    internal fun `hentUttrekkArbeidssøkere - ekstra fagsak`() {
        opprettdata()
        opprettEkstraFagsak()

        service.opprettUttrekkArbeidssøkere(mars2021)

        val uttrekk = service.hentUttrekkArbeidssøkere(mars2021).arbeidssøkere
        assertThat(uttrekk).hasSize(2)
    }

    @Nested
    inner class `Registrert som arbeidssøkere` {

        @Test
        internal fun `hentUttrekkArbeidssøkere - er registrert som arbeidssøker hvis det finnes periode siste dagen i måneden`() {
            listOf(ArbeidssøkerPeriode(mars2021.atDay(1), mars2021.atEndOfMonth()),
                   ArbeidssøkerPeriode(mars2021.atEndOfMonth(), mars2021.atEndOfMonth()),
                   ArbeidssøkerPeriode(mars2021.atEndOfMonth(), mars2021.atEndOfMonth().plusDays(1))).forEach {
                every { arbeidssøkerClient.hentPerioder(any(), any(), any()) } returns ArbeidssøkerResponse(listOf(it))

                opprettdata()

                service.opprettUttrekkArbeidssøkere(mars2021)
                assertThat(uttrekkArbeidssøkerRepository.findAll().single().registrertArbeidssøker).isTrue

                testWithSaksbehandlerContext {
                    val uttrekk = service.hentUttrekkArbeidssøkere(mars2021).arbeidssøkere
                    assertThat(uttrekk).isEmpty()
                }
                reset()
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - er ikke registrert hvis det ikke finnes periode siste dagen i måneden`() {
            listOf(ArbeidssøkerPeriode(mars2021.atDay(1), mars2021.atEndOfMonth().minusDays(1)),
                   ArbeidssøkerPeriode(mars2021.atEndOfMonth().plusDays(1), mars2021.atEndOfMonth().plusMonths(1))).forEach {
                every { arbeidssøkerClient.hentPerioder(any(), any(), any()) } returns ArbeidssøkerResponse(listOf(it))

                opprettdata()

                service.opprettUttrekkArbeidssøkere(mars2021)

                testWithSaksbehandlerContext {
                    val uttrekk = service.hentUttrekkArbeidssøkere(mars2021).arbeidssøkere
                    assertThat(uttrekk[0].registrertArbeidssøker).isFalse
                }
                reset()
            }
        }

    }

    @Test
    internal fun `hentUttrekkArbeidssøkere - skal inkludere de som er kontrollert og`() {
        opprettdata()
        for (i in 1..20) {
            val arbeidssøkere = UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                     vedtakId = behandling.id,
                                                     årMåned = mars2021,
                                                     registrertArbeidssøker = false)
            uttrekkArbeidssøkerRepository.insert(arbeidssøkere)
        }
        for (i in 1..2) {
            val arbeidssøkere =
                    UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                         vedtakId = behandling.id,
                                         årMåned = mars2021,
                                         kontrollert = true,
                                         registrertArbeidssøker = false)
            uttrekkArbeidssøkerRepository.insert(arbeidssøkere)
        }

        testWithSaksbehandlerContext {
            val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)
            assertThat(uttrekk.antallTotalt).isEqualTo(22)
            assertThat(uttrekk.antallKontrollert).isEqualTo(2)
            assertThat(uttrekk.arbeidssøkere.size).isEqualTo(20)

            val uttrekkMedKontrollerte = service.hentUttrekkArbeidssøkere(mars2021, visKontrollerte = true)
            assertThat(uttrekkMedKontrollerte.antallTotalt).isEqualTo(22)
            assertThat(uttrekkMedKontrollerte.antallKontrollert).isEqualTo(2)
            assertThat(uttrekkMedKontrollerte.arbeidssøkere.size).isEqualTo(22)
        }
    }

    @Nested
    inner class Tilgangstester {

        private val IDENT_STRENGT_FORTROLIG_UTLAND = "1"
        private val IDENT_STRENGT_FORTROLIG = "2"
        private val IDENT_FORTROLIG = "3"
        private val IDENT_UGRADERT = "4"
        private val IDENT_UTEN_GRADERING = "5"

        private val identer = listOf(IDENT_STRENGT_FORTROLIG_UTLAND to AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
                                     IDENT_STRENGT_FORTROLIG to AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                                     IDENT_FORTROLIG to AdressebeskyttelseGradering.FORTROLIG,
                                     IDENT_UGRADERT to AdressebeskyttelseGradering.UGRADERT,
                                     IDENT_UTEN_GRADERING to null)

        @BeforeEach
        internal fun setUp() {
            every { personService.hentPdlPersonKort(any()) } answers {
                identer.associate { it.first to lagPersonKort(it.second) }
            }
            identer.forEach {
                val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf(it.first))))
                val behandling = behandlingRepository.insert(behandling(fagsak))
                val arbeidssøkere = UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                         vedtakId = behandling.id,
                                                         årMåned = mars2021,
                                                         registrertArbeidssøker = false)
                uttrekkArbeidssøkerRepository.insert(arbeidssøkere)
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - uten rolle filtrerer vekk personer som man ikke har tilgang til`() {
            val expected = listOf(IDENT_UGRADERT, IDENT_UTEN_GRADERING)
            testWithSaksbehandlerContext {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)
                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(2)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(3)
                assertThat(uttrekk.arbeidssøkere).hasSize(2)
                validateAdressebeskyttelse(uttrekk, IDENT_UGRADERT, DtoAdressebeskyttelse.UGRADERT)
                validateAdressebeskyttelse(uttrekk, IDENT_UTEN_GRADERING, null)
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - kode 6 tilgang`() {
            val expected = listOf(IDENT_STRENGT_FORTROLIG, IDENT_STRENGT_FORTROLIG_UTLAND)
            testWithSaksbehandlerContext(groups = listOf(rolleConfig.kode6)) {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)

                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(2)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(3)
                assertThat(uttrekk.arbeidssøkere).hasSize(2)
                validateAdressebeskyttelse(uttrekk, IDENT_STRENGT_FORTROLIG, DtoAdressebeskyttelse.STRENGT_FORTROLIG)
                validateAdressebeskyttelse(uttrekk,
                                           IDENT_STRENGT_FORTROLIG_UTLAND,
                                           DtoAdressebeskyttelse.STRENGT_FORTROLIG_UTLAND)
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - kode 7 tilgang`() {
            val expected = listOf(IDENT_UGRADERT, IDENT_UTEN_GRADERING, IDENT_FORTROLIG)
            testWithSaksbehandlerContext(groups = listOf(rolleConfig.kode7)) {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)

                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(3)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(2)
                assertThat(uttrekk.arbeidssøkere).hasSize(3)
                validateAdressebeskyttelse(uttrekk, IDENT_FORTROLIG, DtoAdressebeskyttelse.FORTROLIG)
                validateAdressebeskyttelse(uttrekk, IDENT_UGRADERT, DtoAdressebeskyttelse.UGRADERT)
                validateAdressebeskyttelse(uttrekk, IDENT_UTEN_GRADERING, null)
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - uten rolle og en kode6-arbeidsøker er kontrollert`() {
            val expected = listOf(IDENT_UGRADERT, IDENT_UTEN_GRADERING)
            fagsakRepository.findBySøkerIdent(setOf(IDENT_STRENGT_FORTROLIG))
                    .single()
                    .let { fagsak ->
                        uttrekkArbeidssøkerRepository.findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(mars2021)
                                .single { it.fagsakId == fagsak.id }
                    }
                    .let { uttrekkArbeidssøkerRepository.update(it.copy(kontrollert = true)) }
            testWithSaksbehandlerContext {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)
                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(2)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(2)
                assertThat(uttrekk.arbeidssøkere).hasSize(2)
                validateAdressebeskyttelse(uttrekk, IDENT_UGRADERT, DtoAdressebeskyttelse.UGRADERT)
                validateAdressebeskyttelse(uttrekk, IDENT_UTEN_GRADERING, null)
            }
        }

        private fun validateAdressebeskyttelse(uttrekk: UttrekkArbeidssøkereDto,
                                               ident: String,
                                               adressebeskyttelse: DtoAdressebeskyttelse?) {
            assertThat(uttrekk.arbeidssøkere.filter { it.personIdent == ident }.map { it.adressebeskyttelse })
                    .containsExactly(adressebeskyttelse)
        }

        private fun validerInneholderIdenter(uttrekk: UttrekkArbeidssøkereDto, identer: List<String>) {
            assertThat(uttrekk.arbeidssøkere.map { it.personIdent }).containsExactlyInAnyOrderElementsOf(identer)
        }
    }

    @Test
    internal fun `settKontrollert - sett arbeidssøker til kontrollert`() {
        opprettdata()
        service.opprettUttrekkArbeidssøkere(mars2021)
        val id = service.hentUttrekkArbeidssøkere(mars2021).arbeidssøkere.single().id

        testWithSaksbehandlerContext { service.settKontrollert(id, true) }

        val oppdatertUttrekk = service.hentUttrekkArbeidssøkere(mars2021, visKontrollerte = true)
        assertThat(oppdatertUttrekk.antallKontrollert).isEqualTo(1)
        assertThat(oppdatertUttrekk.antallTotalt).isEqualTo(1)
        assertThat(oppdatertUttrekk.arbeidssøkere).hasSize(1)
        assertThat(oppdatertUttrekk.arbeidssøkere[0].kontrollert).isTrue

        testWithSaksbehandlerContext { service.settKontrollert(id, false) }
        val oppdatertUttrekk2 = service.hentUttrekkArbeidssøkere(mars2021)
        assertThat(oppdatertUttrekk2.antallKontrollert).isEqualTo(0)
        assertThat(oppdatertUttrekk2.arbeidssøkere[0].kontrollert).isFalse
    }

    @Test
    internal fun `settKontrollert - har ikke tilgang til fagsak`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("ikkeTilgang"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val uttrekkArbeidssøkere = UttrekkArbeidssøkere(fagsakId = fagsak.id, vedtakId = behandling.id, årMåned = mars2021)
        val uttrekk = uttrekkArbeidssøkerRepository.insert(uttrekkArbeidssøkere)

        assertThrows<ManglerTilgang> {
            testWithSaksbehandlerContext {
                service.settKontrollert(uttrekk.id, true)
            }
        }
    }

    private fun opprettdata() {
        opprettBehandlinger()
        innvilg(behandling, listOf(vedtaksperiode))
        ferdigstillBehandling(behandling)
        innvilg(behandling2,
                listOf(vedtaksperiode2, vedtaksperiode3),
                listOf(Inntekt(februar2021, BigDecimal.ZERO, BigDecimal(10_000))))
        ferdigstillBehandling(behandling2)
    }

    private fun opprettEkstraFagsak() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("2"))))
        val behandling = behandlingRepository.insert(
                behandling(fagsak = fagsak,
                           type = BehandlingType.REVURDERING,
                           forrigeBehandlingId = behandling2.id,
                           opprettetTid = behandling2.sporbar.opprettetTid.plusDays(1)))
        innvilg(behandling,
                listOf(vedtaksperiode2, vedtaksperiode3),
                listOf(Inntekt(februar2021, BigDecimal.ZERO, BigDecimal(15_000))))
        ferdigstillBehandling(behandling)
    }

    fun ferdigstillBehandling(behandling: Behandling) {
        behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT,
                                                    resultat = BehandlingResultat.INNVILGET))
    }

    private fun opprettVedtaksperiode(fra: YearMonth,
                                      til: YearMonth,
                                      aktivitetType: AktivitetType = AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER) =
            VedtaksperiodeDto(fra, til, aktivitetType, VedtaksperiodeType.PERIODE_FØR_FØDSEL)

    private fun innvilg(behandling: Behandling,
                        vedtaksperioder: List<VedtaksperiodeDto>,
                        inntekter: List<Inntekt> = listOf(Inntekt(vedtaksperioder.first().årMånedFra, null, null))) {
        val vedtak = Innvilget(resultatType = ResultatType.INNVILGE,
                               perioder = vedtaksperioder,
                               inntekter = inntekter,
                               periodeBegrunnelse = null,
                               inntektBegrunnelse = null)
        beregnYtelseSteg.utførSteg(behandling, vedtak)
    }

    fun opprettBehandlinger() {
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        behandlingRepository.insert(behandling2)
    }

    private fun lagPersonKort(gradering: AdressebeskyttelseGradering? = null) =
            PdlPersonKort(gradering?.let { listOf(Adressebeskyttelse(it, Metadata(false))) } ?: emptyList(),
                          listOf(navn),
                          emptyList())

    fun testWithSaksbehandlerContext(groups: List<String> = emptyList(), fn: () -> Unit) {
        testWithBrukerContext(groups = groups + rolleConfig.saksbehandlerRolle, fn = fn)
    }
}
