package no.nav.familie.ef.sak.vedtak.uttrekk

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.exception.ManglerTilgang
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker.ArbeidssøkerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker.ArbeidssøkerPeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker.LocalDateWrapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType.BARNET_ER_SYKT
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.tilDomene
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Adressebeskyttelse as DtoAdressebeskyttelse

internal class UttrekkArbeidssøkerServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var tilgangService: TilgangService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var uttrekkArbeidssøkerRepository: UttrekkArbeidssøkerRepository

    @Autowired
    private lateinit var beregnYtelseSteg: BeregnYtelseSteg

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    private lateinit var opprettUttrekkArbeidssøkerTask: OpprettUttrekkArbeidssøkerTask

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vurderingService: VurderingService

    private lateinit var service: UttrekkArbeidssøkerService
    private val arbeidssøkerClient = mockk<ArbeidssøkerClient>()
    private val personService = mockk<PersonService>()
    private val taskService = mockk<TaskService>()
    private val fagsak = fagsak(fagsakpersoner(setOf("1")))
    private val behandling = behandling(fagsak)
    private val behandling2 =
        behandling(
            fagsak,
            type = BehandlingType.REVURDERING,
            forrigeBehandlingId = behandling.id,
            opprettetTid = behandling.sporbar.opprettetTid.plusDays(1),
        )

    private val januar2021 = YearMonth.of(2021, 1)
    private val februar2021 = YearMonth.of(2021, 2)
    private val mars2021 = YearMonth.of(2021, 3)

    private val vedtaksperiode = opprettVedtaksperiode(januar2021, mars2021)
    private val vedtaksperiode2 =
        opprettVedtaksperiode(
            februar2021,
            februar2021,
            aktivitetType = BARNET_ER_SYKT,
        )
    private val vedtaksperiode3 =
        opprettVedtaksperiode(
            mars2021,
            mars2021,
            aktivitetType = FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER,
        )
    private val navn = Navn("fornavn", "", "", Metadata(false))

    private val taskSlot = slot<Task>()

    @BeforeEach
    internal fun setUp() {
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
        every { arbeidssøkerClient.hentPerioder(any()) } returns listOf()
        every { personService.hentPersonKortBolk(any()) } answers {
            firstArg<List<String>>().associateWith { lagPersonKort() }
        }
        service =
            UttrekkArbeidssøkerService(
                tilgangService,
                uttrekkArbeidssøkerRepository,
                fagsakService,
                personService,
                arbeidssøkerClient,
                vurderingService,
            )
        opprettUttrekkArbeidssøkerTask =
            OpprettUttrekkArbeidssøkerTask(service, fagsakService, taskService)
    }

    @Test
    internal fun `hentUttrekkArbeidssøkere - Filtrer vekk EØS-personer`() {
        val identer = listOf("1", "2", "3")
        identer.forEach {
            val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf(it))))
            val behandling = behandlingRepository.insert(behandling(fagsak))

            if (it == "2") {
                val vilkårsvurderingLovligOpphold =
                    leggTilDelvilkårEøsKnyttetBehandling(behandling)
                vilkårsvurderingRepository.insert(vilkårsvurderingLovligOpphold)
            } else {
                val vilkår = vilkårsvurdering(behandling.id)
                vilkårsvurderingRepository.insert(vilkår)
            }

            val arbeidssøkere =
                UttrekkArbeidssøkere(
                    fagsakId = fagsak.id,
                    vedtakId = behandling.id,
                    årMåned = mars2021,
                    registrertArbeidssøker = false,
                )
            uttrekkArbeidssøkerRepository.insert(arbeidssøkere)
        }

        val expected = listOf("1", "3")
        testWithSaksbehandlerContext {
            val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)
            assertThat(uttrekk.arbeidssøkere.map { it.personIdent }).containsExactlyInAnyOrderElementsOf(expected)
            val utrekkEøsBorgere = service.hentUttrekkArbeidssøkere(mars2021, visEøsBorgere = true)
            assertThat(utrekkEøsBorgere.antallTotalt).isEqualTo(1)
            assertThat(utrekkEøsBorgere.arbeidssøkere.first().personIdent).isEqualTo("2")
        }
    }

    private fun leggTilDelvilkårEøsKnyttetBehandling(behandling: Behandling): Vilkårsvurdering =
        vilkårsvurdering(
            behandlingId = behandling.id,
            type = VilkårType.LOVLIG_OPPHOLD,
            delvilkårsvurdering =
                listOf(
                    Delvilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT, vurderinger = listOf(Vurdering(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI))),
                    Delvilkårsvurdering(resultat = Vilkårsresultat.OPPFYLT, vurderinger = listOf(Vurdering(RegelId.OPPHOLD_UNNTAK, SvarId.OPPHOLDER_SEG_I_ANNET_EØS_LAND))),
                ),
        )

    @Test
    internal fun `skal kjøre query uten problemer`() {
        assertThat(service.hentArbeidssøkereForUttrekk(YearMonth.now())).isEmpty()
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
        opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(februar2021))
        assertThat(service.hentUttrekkArbeidssøkere(februar2021).arbeidssøkere).isEmpty()
    }

    @Test
    internal fun `hentUttrekkArbeidssøkere - skal opprette uttrekk for arbeidssøkere`() {
        opprettdata()

        opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(januar2021))

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
        opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(mars2021))

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

        opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(mars2021))
        val uttrekk = service.hentUttrekkArbeidssøkere(mars2021).arbeidssøkere
        assertThat(uttrekk).hasSize(2)
    }

    @Nested
    inner class `Registrert som arbeidssøkere` {
        @Test
        internal fun `hentUttrekkArbeidssøkere - er registrert som arbeidssøker hvis det finnes periode siste dagen i måneden`() {
            listOf(
                ArbeidssøkerPeriode(startet = LocalDateWrapper(mars2021.atDay(1).atStartOfDay()), avsluttet = LocalDateWrapper(mars2021.atEndOfMonth().atStartOfDay())),
                ArbeidssøkerPeriode(startet = LocalDateWrapper(mars2021.atEndOfMonth().atStartOfDay()), avsluttet = LocalDateWrapper(mars2021.atEndOfMonth().atStartOfDay())),
                ArbeidssøkerPeriode(startet = LocalDateWrapper(mars2021.atEndOfMonth().atStartOfDay()), avsluttet = LocalDateWrapper(mars2021.atEndOfMonth().plusDays(1).atStartOfDay())),
            ).forEach {
                every { arbeidssøkerClient.hentPerioder(any()) } returns listOf(it)

                opprettdata()

                opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(mars2021))
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
            listOf(
                ArbeidssøkerPeriode(startet = LocalDateWrapper(mars2021.atDay(1).atStartOfDay()), avsluttet = LocalDateWrapper(mars2021.atEndOfMonth().minusDays(1).atStartOfDay())),
                ArbeidssøkerPeriode(startet = LocalDateWrapper(mars2021.atEndOfMonth().plusDays(1).atStartOfDay()), avsluttet = LocalDateWrapper(mars2021.atEndOfMonth().plusMonths(1).atStartOfDay())),
            ).forEach {
                every { arbeidssøkerClient.hentPerioder(any()) } returns listOf(it)

                opprettdata()

                opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(mars2021))

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
            val arbeidssøkere =
                UttrekkArbeidssøkere(
                    fagsakId = fagsak.id,
                    vedtakId = behandling.id,
                    årMåned = mars2021,
                    registrertArbeidssøker = false,
                )
            uttrekkArbeidssøkerRepository.insert(arbeidssøkere)
        }
        for (i in 1..2) {
            val arbeidssøkere =
                UttrekkArbeidssøkere(
                    fagsakId = fagsak.id,
                    vedtakId = behandling.id,
                    årMåned = mars2021,
                    kontrollert = true,
                    registrertArbeidssøker = false,
                )
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

    @Test
    internal fun `manglende aktiv ident ved opprettUttrekkforArbeissøkere i første kall, forvent insert av kun denne andre gang`() {
        val uttrekkSlot = slot<UttrekkArbeidssøkere>()
        val tilgangService = mockk<TilgangService>()
        val mockUttrekkArbeidssøkerRepository = mockk<UttrekkArbeidssøkerRepository>()
        val mockFagsakService = mockk<FagsakService>()
        val uttrekkArbeidssøkerService =
            UttrekkArbeidssøkerService(
                tilgangService,
                mockUttrekkArbeidssøkerRepository,
                mockFagsakService,
                personService,
                arbeidssøkerClient,
                vurderingService,
            )
        val opprettUttrekkArbeidssøkerTask =
            OpprettUttrekkArbeidssøkerTask(uttrekkArbeidssøkerService, mockFagsakService, taskService)

        val arbeidssøkerPeriode = ArbeidssøkerPeriode(startet = LocalDateWrapper(vedtaksperiode.periode.fomDato.atTime(23,59)), avsluttet = LocalDateWrapper(vedtaksperiode.periode.tomDato.atTime(23,59)))
        val periodeForUttrekk =
            VedtaksperioderForUttrekk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                PeriodeWrapper(listOf(vedtaksperiode).tilDomene()),
            )
        val periodeForUttrekk2 =
            VedtaksperioderForUttrekk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                PeriodeWrapper(listOf(vedtaksperiode).tilDomene()),
            )

        val aktiveIdenter = mutableMapOf(periodeForUttrekk2.fagsakId to "")

        every { mockFagsakService.hentAktiveIdenter(any()) } returns aktiveIdenter
        every { mockUttrekkArbeidssøkerRepository.insert(capture(uttrekkSlot)) } returns mockk()
        every { arbeidssøkerClient.hentPerioder(any()) } returns listOf(arbeidssøkerPeriode, arbeidssøkerPeriode)


        every {
            mockUttrekkArbeidssøkerRepository.hentVedtaksperioderForSisteFerdigstilteBehandlinger(any(), any())
        } returns
                listOf(
                    periodeForUttrekk,
                    periodeForUttrekk2,
                )

        every {
            mockUttrekkArbeidssøkerRepository.existsByÅrMånedAndFagsakId(any(), any())
        } returns false andThen false andThen false andThen true

        Assertions.assertThrows(IllegalStateException::class.java) {
            opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(mars2021))
        }
        verify(exactly = 1) { mockUttrekkArbeidssøkerRepository.insert(any()) }
        assertThat(uttrekkSlot.captured.fagsakId).isEqualTo(periodeForUttrekk2.fagsakId)

        aktiveIdenter.put(periodeForUttrekk.fagsakId, "")
        opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(mars2021))
        verify(exactly = 2) { mockUttrekkArbeidssøkerRepository.insert(any()) }
        assertThat(uttrekkSlot.captured.fagsakId).isEqualTo(periodeForUttrekk.fagsakId)
    }

    @Nested
    inner class Tilgangstester {
        private val identStrengtFortroligUtland = "1"
        private val identStrengtFortrolig = "2"
        private val identFortrolig = "3"
        private val identUgradert = "4"
        private val identUtenGradering = "5"

        private val identer =
            listOf(
                identStrengtFortroligUtland to AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
                identStrengtFortrolig to AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                identFortrolig to AdressebeskyttelseGradering.FORTROLIG,
                identUgradert to AdressebeskyttelseGradering.UGRADERT,
                identUtenGradering to null,
            )

        @BeforeEach
        internal fun setUp() {
            every { personService.hentPersonKortBolk(any()) } answers {
                identer.associate { it.first to lagPersonKort(it.second) }
            }
            identer.forEach {
                val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf(it.first))))
                val behandling = behandlingRepository.insert(behandling(fagsak))
                val vilkår = vilkårsvurdering(behandling.id)
                vilkårsvurderingRepository.insert(vilkår)
                val arbeidssøkere =
                    UttrekkArbeidssøkere(
                        fagsakId = fagsak.id,
                        vedtakId = behandling.id,
                        årMåned = mars2021,
                        registrertArbeidssøker = false,
                    )
                uttrekkArbeidssøkerRepository.insert(arbeidssøkere)
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - uten rolle filtrerer vekk personer som man ikke har tilgang til`() {
            val expected = listOf(identUgradert, identUtenGradering)
            testWithSaksbehandlerContext {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)
                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(2)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(3)
                assertThat(uttrekk.arbeidssøkere).hasSize(2)
                validateAdressebeskyttelse(uttrekk, identUgradert, DtoAdressebeskyttelse.UGRADERT)
                validateAdressebeskyttelse(uttrekk, identUtenGradering, null)
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - kode 6 tilgang`() {
            val expected = listOf(identStrengtFortrolig, identStrengtFortroligUtland)
            testWithSaksbehandlerContext(groups = listOf(rolleConfig.kode6)) {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)

                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(2)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(3)
                assertThat(uttrekk.arbeidssøkere).hasSize(2)
                validateAdressebeskyttelse(uttrekk, identStrengtFortrolig, DtoAdressebeskyttelse.STRENGT_FORTROLIG)
                validateAdressebeskyttelse(
                    uttrekk,
                    identStrengtFortroligUtland,
                    DtoAdressebeskyttelse.STRENGT_FORTROLIG_UTLAND,
                )
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - kode 7 tilgang`() {
            val expected = listOf(identUgradert, identUtenGradering, identFortrolig)
            testWithSaksbehandlerContext(groups = listOf(rolleConfig.kode7)) {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)

                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(3)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(2)
                assertThat(uttrekk.arbeidssøkere).hasSize(3)
                validateAdressebeskyttelse(uttrekk, identFortrolig, DtoAdressebeskyttelse.FORTROLIG)
                validateAdressebeskyttelse(uttrekk, identUgradert, DtoAdressebeskyttelse.UGRADERT)
                validateAdressebeskyttelse(uttrekk, identUtenGradering, null)
            }
        }

        @Test
        internal fun `hentUttrekkArbeidssøkere - uten rolle og en kode6-arbeidsøker er kontrollert`() {
            val expected = listOf(identUgradert, identUtenGradering)
            fagsakRepository
                .findBySøkerIdent(setOf(identStrengtFortrolig))
                .single()
                .let { fagsak ->
                    uttrekkArbeidssøkerRepository
                        .findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(mars2021)
                        .single { it.fagsakId == fagsak.id }
                }.let { uttrekkArbeidssøkerRepository.update(it.copy(kontrollert = true)) }
            testWithSaksbehandlerContext {
                val uttrekk = service.hentUttrekkArbeidssøkere(mars2021)
                validerInneholderIdenter(uttrekk, expected)
                assertThat(uttrekk.antallTotalt).isEqualTo(2)
                assertThat(uttrekk.antallManglerKontrollUtenTilgang).isEqualTo(2)
                assertThat(uttrekk.arbeidssøkere).hasSize(2)
                validateAdressebeskyttelse(uttrekk, identUgradert, DtoAdressebeskyttelse.UGRADERT)
                validateAdressebeskyttelse(uttrekk, identUtenGradering, null)
            }
        }

        private fun validateAdressebeskyttelse(
            uttrekk: UttrekkArbeidssøkereDto,
            ident: String,
            adressebeskyttelse: DtoAdressebeskyttelse?,
        ) {
            assertThat(uttrekk.arbeidssøkere.filter { it.personIdent == ident }.map { it.adressebeskyttelse })
                .containsExactly(adressebeskyttelse)
        }

        private fun validerInneholderIdenter(
            uttrekk: UttrekkArbeidssøkereDto,
            identer: List<String>,
        ) {
            assertThat(uttrekk.arbeidssøkere.map { it.personIdent }).containsExactlyInAnyOrderElementsOf(identer)
        }
    }

    @Test
    internal fun `settKontrollert - sett arbeidssøker til kontrollert`() {
        opprettdata()
        opprettUttrekkArbeidssøkerTask.doTask(OpprettUttrekkArbeidssøkerTask.opprettTask(mars2021))
        val id =
            service
                .hentUttrekkArbeidssøkere(mars2021)
                .arbeidssøkere
                .single()
                .id

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

    @Test
    internal fun `oppretter task som kjører neste måned`() {
        val now = YearMonth.now()
        val task = OpprettUttrekkArbeidssøkerTask.opprettTask(now)
        assertThat(task.payload).isEqualTo(now.toString())
        assertThat(task.triggerTid).isEqualTo(now.plusMonths(1).atDay(1).atTime(5, 0))

        opprettUttrekkArbeidssøkerTask.onCompletion(task)
        val lagretTask = taskSlot.captured
        assertThat(lagretTask.payload).isEqualTo(now.plusMonths(1).toString())
        assertThat(lagretTask.triggerTid).isEqualTo(now.plusMonths(2).atDay(1).atTime(5, 0))
    }


    private fun opprettdata() {
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        innvilg(fagsak, behandling, listOf(vedtaksperiode))
        ferdigstillBehandling(behandling)

        behandlingRepository.insert(behandling2)
        innvilg(
            fagsak,
            behandling2,
            listOf(vedtaksperiode2, vedtaksperiode3),
            listOf(Inntekt(februar2021, BigDecimal.ZERO, BigDecimal(10_000))),
        )
        ferdigstillBehandling(behandling2)
        val vilkår = vilkårsvurdering(behandling.id)
        vilkårsvurderingRepository.insert(vilkår)
        val vilkår2 = vilkårsvurdering(behandling2.id)
        vilkårsvurderingRepository.insert(vilkår2)
    }

    private fun opprettEkstraFagsak() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("2"))))
        val behandling =
            behandlingRepository.insert(
                behandling(
                    fagsak = fagsak,
                    type = BehandlingType.REVURDERING,
                    forrigeBehandlingId = behandling2.id,
                    opprettetTid = behandling2.sporbar.opprettetTid.plusDays(1),
                ),
            )
        innvilg(
            fagsak,
            behandling,
            listOf(vedtaksperiode2, vedtaksperiode3),
            listOf(Inntekt(februar2021, BigDecimal.ZERO, BigDecimal(15_000))),
        )
        ferdigstillBehandling(behandling)
        val vilkår = vilkårsvurdering(behandling.id)
        vilkårsvurderingRepository.insert(vilkår)
    }

    fun ferdigstillBehandling(behandling: Behandling) {
        behandlingRepository.update(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
                vedtakstidspunkt = SporbarUtils.now(),
            ),
        )
    }

    private fun opprettVedtaksperiode(
        fra: YearMonth,
        til: YearMonth,
        aktivitetType: AktivitetType = AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER,
    ) = VedtaksperiodeDto(fra, til, Månedsperiode(fra, til), aktivitetType, VedtaksperiodeType.PERIODE_FØR_FØDSEL)

    private fun innvilg(
        fagsak: Fagsak,
        behandling: Behandling,
        vedtaksperioder: List<VedtaksperiodeDto>,
        inntekter: List<Inntekt> = listOf(Inntekt(vedtaksperioder.first().periode.fom, null, null)),
    ) {
        val vedtak =
            InnvilgelseOvergangsstønad(
                perioder = vedtaksperioder,
                inntekter = inntekter,
                periodeBegrunnelse = null,
                inntektBegrunnelse = null,
            )
        beregnYtelseSteg.utførSteg(saksbehandling(fagsak, behandling), vedtak)
    }

    private fun lagPersonKort(gradering: AdressebeskyttelseGradering? = null) =
        PdlPersonKort(
            gradering?.let { listOf(Adressebeskyttelse(it, Metadata(false))) } ?: emptyList(),
            listOf(navn),
            emptyList(),
        )

    fun testWithSaksbehandlerContext(
        groups: List<String> = emptyList(),
        fn: () -> Unit,
    ) {
        testWithBrukerContext(groups = groups + rolleConfig.saksbehandlerRolle, fn = fn)
    }
}
