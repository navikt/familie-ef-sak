package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.revurdering.AutomatiskRevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.BehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.PayloadBehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.RevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.tilNorskFormat
import no.nav.familie.ef.sak.behandling.revurdering.tilNorskFormatUtenÅr
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.YEAR_MONTH_MAX
import no.nav.familie.ef.sak.infrastruktur.config.InntektClientMock
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.inntekt
import no.nav.familie.ef.sak.repository.inntektsmåned
import no.nav.familie.ef.sak.repository.inntektsmåneder
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.lagInntektResponseFraMånedsinntekter
import no.nav.familie.ef.sak.repository.lagInntektResponseFraMånedsinntekterFraDouble
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.testutil.VedtakHelperService
import no.nav.familie.ef.sak.testutil.VilkårHelperService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth

class BehandleAutomatiskInntektsendringTaskTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var featureToggleService: FeatureToggleService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var revurderingService: RevurderingService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var årsakRevurderingsRepository: ÅrsakRevurderingsRepository

    @Autowired
    private lateinit var automatiskRevurderingService: AutomatiskRevurderingService

    @Autowired
    private lateinit var vilkårHelperService: VilkårHelperService

    @Autowired
    private lateinit var vedtakHelperService: VedtakHelperService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var inntektClientMock: InntektClientMock

    private val personIdent = "3"
    private val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)))

    private lateinit var behandleAutomatiskInntektsendringTask: BehandleAutomatiskInntektsendringTask

    @BeforeEach
    fun setup() {
        testoppsettService.lagreFagsak(fagsak)
        behandleAutomatiskInntektsendringTask =
            BehandleAutomatiskInntektsendringTask(
                revurderingService = revurderingService,
                fagsakService = fagsakService,
                behandlingService = behandlingService,
                vedtakService = vedtakService,
                årsakRevurderingsRepository = årsakRevurderingsRepository,
                automatiskRevurderingService = automatiskRevurderingService,
                featureToggleService = featureToggleService,
            )
    }

    @Test
    fun `behandling automatisk inntektsendring`() {
        val inntekterPerMånedLavInntekt = listOf(inntekt(2000.0), inntekt(10000.0))
        val inntektsmånederLavInntekt = inntektsmåneder(fraOgMedMåned = YearMonth.now().minusMonths(12), tilOgMedMåned = YearMonth.now().minusMonths(4), inntektListe = inntekterPerMånedLavInntekt)
        val inntekterPerMånedHøyInntekt = listOf(inntekt(25000.0), inntekt(10000.0))
        val inntektsmånederHøyInntekt = inntektsmåneder(fraOgMedMåned = YearMonth.now().minusMonths(3), inntektListe = inntekterPerMånedHøyInntekt)
        every { inntektClientMock.inntektClient().hentInntekt(any(), any(), any()) } returns InntektResponse(inntektsmånederLavInntekt + inntektsmånederHøyInntekt)

        val behandling = behandling(fagsak, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES)
        behandlingRepository.insert(behandling)
        vilkårHelperService.opprettVilkår(behandling)
        vedtakHelperService.ferdigstillVedtak(vedtak(behandlingId = behandling.id, månedsperiode = Månedsperiode(YearMonth.now().minusMonths(4), YearMonth.now())), behandling, fagsak)

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-15")
        val task = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))
        val lagretBehandleAutomatiskInntektsendringTask = taskService.save(task)
        behandleAutomatiskInntektsendringTask.doTask(lagretBehandleAutomatiskInntektsendringTask)

        val behandlingerForFagsak = behandlingRepository.findByFagsakId(fagsakId = fagsak.id)
        assertThat(behandlingerForFagsak).hasSize(2)
        val automatiskInntektsendringBehandling = behandlingerForFagsak.first { it.årsak == BehandlingÅrsak.AUTOMATISK_INNTEKTSENDRING }
        val årsakTilRevurdering = årsakRevurderingsRepository.findByIdOrThrow(automatiskInntektsendringBehandling.id)

        assertThat(årsakTilRevurdering.opplysningskilde).isEqualTo(Opplysningskilde.AUTOMATISK_OPPRETTET_BEHANDLING)
        assertThat(årsakTilRevurdering.årsak).isEqualTo(Revurderingsårsak.ENDRING_INNTEKT)
        assertThat(årsakTilRevurdering.beskrivelse).isNullOrEmpty()

        val vedtak = vedtakService.hentVedtak(automatiskInntektsendringBehandling.id)
        val vedtaksperioder = vedtak.perioder?.perioder
        val førsteFom = vedtaksperioder?.first()?.periode?.fom
        val inntektsperioder = vedtak.inntekter?.inntekter

        assertThat(førsteFom).isEqualTo(YearMonth.now().minusMonths(2))
        assertThat(inntektsperioder?.first()?.månedsinntekt?.toInt()).isEqualTo(35_000)

        val opprettOppgaveTask = taskService.findAll().first { it.type == OpprettOppgaveForOpprettetBehandlingTask.TYPE }
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(opprettOppgaveTask.payload)
        assertThat(data.mappeId).isNull() // Blir satt på senere tidspunkt
        assertThat(data.beskrivelse).isEqualTo("Automatisk opprettet revurdering som følge av inntektskontroll")
    }

    @Test
    fun `Sett revurderes fra dato måneden etter inntektsøkning - opprett inntektsperioder for hver måned tilbake i tid`() {
        val innmeldtMånedsinntekt = listOf(10_000, 10_500, 15_000, 15_000, 15_000)
        val vedtakTom = YearMonth.now().plusMonths(11)

        val forventetInntektIVedtak =
            mapOf(
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong()) to 10_000),
            )
        val vedtak = vedtak(forventetInntektIVedtak, vedtakTom)
        val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)

        val oppdatertVedtakMedNyePerioder = behandleAutomatiskInntektsendringTask.oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(vedtak, inntektResponse)

        assertThat(oppdatertVedtakMedNyePerioder.first().periode.fom).isEqualTo(YearMonth.now().minusMonths(2))
        assertThat(oppdatertVedtakMedNyePerioder.first().periode.tom).isEqualTo(vedtakTom)

        val oppdatertInntekt = behandleAutomatiskInntektsendringTask.oppdaterInntektMedNyBeregnetForventetInntekt(vedtak, inntektResponse, oppdatertVedtakMedNyePerioder.first().periode.fom)
        assertThat(oppdatertInntekt.first().periode.fom).isEqualTo(YearMonth.now().minusMonths(2))
        assertThat(oppdatertInntekt.first().månedsinntekt).isEqualTo(BigDecimal(15_000))
    }

    @Test
    fun `en inntektsperiode med flere endringer i inntekt etter 10 prosent økning`() {
        val personIdent = "4"
        val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)))
        val behandling = behandling(fagsak, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES)
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        vilkårHelperService.opprettVilkår(behandling)

        val innmeldtMånedsinntekt = listOf(11_000, 12_000, 12_000, 16_000, 16_000, 24_000, 24_000)
        val vedtakTom = YearMonth.now().plusMonths(11)

        val forventetInntektIVedtak =
            mapOf(
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong()) to 11_000),
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong() - 1) to 12000),
            )
        val vedtak = vedtak(forventetInntektIVedtak, vedtakTom)
        vedtakHelperService.ferdigstillVedtak(vedtak, behandling, fagsak)

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-15")
        val opprettetTask = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))
        val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)

        every { inntektClientMock.inntektClient().hentInntekt("4", any(), any()) } returns inntektResponse

        behandleAutomatiskInntektsendringTask.doTask(opprettetTask)

        val revurdering = behandlingService.hentBehandlinger(fagsak.id).last()
        val oppdatertVedtak = vedtakService.hentVedtak(revurdering.id)

        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.fom,
        ).isEqualTo(YearMonth.now().minusMonths(3))
        assertThat(
            oppdatertVedtak.perioder
                ?.perioder
                ?.first()
                ?.periode
                ?.tom,
        ).isEqualTo(vedtakTom)

        val oppdatertInntekt = oppdatertVedtak.inntekter?.inntekter ?: emptyList()
        assertThat(oppdatertInntekt.size).isEqualTo(3)
        assertThat(oppdatertVedtak.inntektBegrunnelse?.replace('\u00A0', ' ')).isEqualTo(forventetInntektsbegrunnelse)

        val gjennomsnittSiste3Mnd = (24_000 + 24_000 + 16_000) / 3

        val forventedeInntektsperioderINyttVedtak =
            listOf(
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(3)), BigDecimal(16_000)),
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(2), YearMonth.now().minusMonths(1)), BigDecimal(24_000)),
                inntektsperiode(Månedsperiode(YearMonth.now(), YEAR_MONTH_MAX), BigDecimal(gjennomsnittSiste3Mnd)),
            )

        assertThat(oppdatertInntekt).isEqualTo(forventedeInntektsperioderINyttVedtak)
    }

    @Test
    fun `to eksisterende inntektsperioder - sett revurderes fra måneden etter 10 prosent endring`() {
        // Vedtak fra August 2024 -> Juli 2027
        // Inntektsperioder: August 54 534, September 39129
        // Beregnet ny forventet inntekt: 43796
        val innmeldtMånedsinntekt = listOf(54534.36, 39129.14, 36361.58, 37609.86, 41796.68, 43213.59, 44122.90, 44052.95, 43213.59) // Inntekt 43213.59 er 10% over forventet inntekt, altså 4 mnd siden.
        val vedtakTom = YearMonth.now().plusMonths(11)

        val forventetInntektIVedtak =
            mapOf(
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong()) to 54534),
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong() - 1) to 39129),
            )
        val vedtak = vedtak(forventetInntektIVedtak, vedtakTom)
        val inntektResponse = lagInntektResponseFraMånedsinntekterFraDouble(innmeldtMånedsinntekt)

        val oppdatertVedtakMedNyePerioder = behandleAutomatiskInntektsendringTask.oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(vedtak, inntektResponse)

        assertThat(oppdatertVedtakMedNyePerioder.first().periode.fom).isEqualTo(YearMonth.now().minusMonths(3))
        assertThat(oppdatertVedtakMedNyePerioder.first().periode.tom).isEqualTo(vedtakTom)

        val oppdatertInntekt = behandleAutomatiskInntektsendringTask.oppdaterInntektMedNyBeregnetForventetInntekt(vedtak, inntektResponse, oppdatertVedtakMedNyePerioder.first().periode.fom)
        assertThat(oppdatertInntekt.size).isEqualTo(4)

        val gjennomsnittSiste3Mnd = (44122.90 + 44052.95 + 43213.59) / 3

        val forventedeInntektsperioderINyttVedtak =
            listOf(
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(3)), BigDecimal(44122)),
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(2), YearMonth.now().minusMonths(2)), BigDecimal(44052)),
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(1), YearMonth.now().minusMonths(1)), BigDecimal(43213)),
                inntektsperiode(Månedsperiode(YearMonth.now(), vedtakTom), BigDecimal(gjennomsnittSiste3Mnd.toInt())),
            )

        assertThat(forventedeInntektsperioderINyttVedtak).isEqualTo(oppdatertInntekt)
    }

    @Test
    fun `ignorer månedsinntekt som tilsvarer under en halv g i årsinntekt`() {
        val innmeldtMånedsinntekt = listOf(0, 3400, 8000, 10_000, 10_000, 12_000) // 8000 er første måned med over 1/2 G i inntekt
        val vedtakTom = YearMonth.now().plusMonths(11)

        val forventetInntektIVedtak =
            mapOf(
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong()) to 0),
            )
        val vedtak = vedtak(forventetInntektIVedtak, vedtakTom)
        val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)

        val oppdatertVedtakMedNyePerioder = behandleAutomatiskInntektsendringTask.oppdaterFørsteVedtaksperiodeMedRevurderesFraDato(vedtak, inntektResponse)

        assertThat(oppdatertVedtakMedNyePerioder.first().periode.fom).isEqualTo(YearMonth.now().minusMonths(3))
        assertThat(oppdatertVedtakMedNyePerioder.first().periode.tom).isEqualTo(vedtakTom)

        val oppdatertInntekt = behandleAutomatiskInntektsendringTask.oppdaterInntektMedNyBeregnetForventetInntekt(vedtak, inntektResponse, oppdatertVedtakMedNyePerioder.first().periode.fom)
        assertThat(oppdatertInntekt.size).isEqualTo(3)

        val gjennomsnittSiste3Mnd = (10_000 + 10_000 + 12_000) / 3

        val forventedeInntektsperioderINyttVedtak =
            listOf(
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(3), YearMonth.now().minusMonths(2)), BigDecimal(10_000)),
                inntektsperiode(Månedsperiode(YearMonth.now().minusMonths(1), YearMonth.now().minusMonths(1)), BigDecimal(12_000)),
                inntektsperiode(Månedsperiode(YearMonth.now(), vedtakTom), BigDecimal(gjennomsnittSiste3Mnd)),
            )

        assertThat(oppdatertInntekt).isEqualTo(forventedeInntektsperioderINyttVedtak)
    }

    @Test
    fun `Skal få riktig begrunnelse for vedtak hvor forventet inntekt var 0 for forrige vedtak`() {
        val personIdent = "4"
        val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)))
        val behandling = behandling(fagsak, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES)
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        vilkårHelperService.opprettVilkår(behandling)

        val innmeldtMånedsinntekt = listOf(12_000, 12_000, 12_000, 16_000, 16_000, 24_000, 24_000)
        val vedtakTom = YearMonth.now().plusMonths(11)

        val forventetInntektIVedtak =
            mapOf(
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong()) to 0),
            )
        val vedtak = vedtak(forventetInntektIVedtak, vedtakTom)
        vedtakHelperService.ferdigstillVedtak(vedtak, behandling, fagsak)

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-15")
        val opprettetTask = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))
        val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)

        every { inntektClientMock.inntektClient().hentInntekt("4", any(), any()) } returns inntektResponse

        behandleAutomatiskInntektsendringTask.doTask(opprettetTask)

        val revurdering = behandlingService.hentBehandlinger(fagsak.id).last()
        val oppdatertVedtak = vedtakService.hentVedtak(revurdering.id)

        assertThat(oppdatertVedtak.inntektBegrunnelse?.replace('\u00A0', ' ')).isEqualTo(forventetInntektsbegrunnelseForrigeVedtak0ForventetInntekt)
    }

    @Test
    fun `Skal få riktig inntektsbegrunnelse for vedtak hvor bruker har hatt utbetaling av feriepenger siste 3 måneder`() {
        val personIdent = "4"
        val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)))
        val behandling = behandling(fagsak, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES)
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        vilkårHelperService.opprettVilkår(behandling)

        val innmeldtMånedsinntekt = listOf(12_000, 12_000, 12_000, 16_000, 16_000, 24_000, 24_000)
        val vedtakTom = YearMonth.now().plusMonths(11)

        val forventetInntektIVedtak =
            mapOf(
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong()) to 11_000),
                (YearMonth.now().minusMonths(innmeldtMånedsinntekt.size.toLong() - 1) to 12000),
            )
        val vedtak = vedtak(forventetInntektIVedtak, vedtakTom)
        vedtakHelperService.ferdigstillVedtak(vedtak, behandling, fagsak)

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-15")
        val opprettetTask = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))
        val inntektResponse = lagInntektResponseFraMånedsinntekter(innmeldtMånedsinntekt)
        val inntektResponseMedFeriepenger = inntektResponse.copy(inntektsmåneder = inntektResponse.inntektsmåneder + inntektsmåned(YearMonth.now().minusMonths(2), inntektListe = listOf(inntekt(beløp = 5000.0, beskrivelse = "feriepenger"))))
        every { inntektClientMock.inntektClient().hentInntekt("4", any(), any()) } returns inntektResponseMedFeriepenger

        behandleAutomatiskInntektsendringTask.doTask(opprettetTask)

        val revurdering = behandlingService.hentBehandlinger(fagsak.id).last()
        val oppdatertVedtak = vedtakService.hentVedtak(revurdering.id)

        assertThat(oppdatertVedtak.inntektBegrunnelse?.replace('\u00A0', ' ')).isEqualTo(forventetInntektsbegrunnelseMedFeriepenger)
    }

    val forventetInntektsbegrunnelseMedFeriepenger =
        """
        Periode som er kontrollert: ${YearMonth.now().minusMonths(7).tilNorskFormat()} til ${YearMonth.now().minusMonths(1).tilNorskFormat()}.
        
        Forventet årsinntekt i ${YearMonth.now().minusMonths(4).tilNorskFormat()}: 144 000 kroner.
        - 10 % opp: 13 200 kroner per måned.
        - 10 % ned: 10 800 kroner per måned.
        
        Inntekten i ${YearMonth.now().minusMonths(4).tilNorskFormat()} er 16 000 kroner. Inntekten har økt minst 10 prosent denne måneden og alle månedene etter dette. Stønaden beregnes på nytt fra måneden etter 10 prosent økning.
        
        Har lagt til grunn faktisk inntekt bakover i tid. Fra og med ${YearMonth.now().tilNorskFormat()} er stønaden beregnet ut ifra gjennomsnittlig inntekt for ${YearMonth.now().minusMonths(3).tilNorskFormatUtenÅr()}, ${YearMonth.now().minusMonths(2).tilNorskFormatUtenÅr()} og ${YearMonth.now().minusMonths(1).tilNorskFormatUtenÅr()}. Bruker har fått utbetalt feriepenger i løpet av siste tre måneder. Disse er ikke tatt med i beregningen av forventet inntekt.
        
        A-inntekt er lagret.
        """.trimIndent()

    val forventetInntektsbegrunnelse =
        """
        Periode som er kontrollert: ${YearMonth.now().minusMonths(7).tilNorskFormat()} til ${YearMonth.now().minusMonths(1).tilNorskFormat()}.
        
        Forventet årsinntekt i ${YearMonth.now().minusMonths(4).tilNorskFormat()}: 144 000 kroner.
        - 10 % opp: 13 200 kroner per måned.
        - 10 % ned: 10 800 kroner per måned.
        
        Inntekten i ${YearMonth.now().minusMonths(4).tilNorskFormat()} er 16 000 kroner. Inntekten har økt minst 10 prosent denne måneden og alle månedene etter dette. Stønaden beregnes på nytt fra måneden etter 10 prosent økning.
        
        Har lagt til grunn faktisk inntekt bakover i tid. Fra og med ${YearMonth.now().tilNorskFormat()} er stønaden beregnet ut ifra gjennomsnittlig inntekt for ${YearMonth.now().minusMonths(3).tilNorskFormatUtenÅr()}, ${YearMonth.now().minusMonths(2).tilNorskFormatUtenÅr()} og ${YearMonth.now().minusMonths(1).tilNorskFormatUtenÅr()}.
        
        A-inntekt er lagret.
        """.trimIndent()

    val forventetInntektsbegrunnelseForrigeVedtak0ForventetInntekt =
        """
        Forventet årsinntekt i ${YearMonth.now().minusMonths(7).tilNorskFormat()}: 0 kroner.
            - Månedsinntekt 1/2 G: 5 423 kroner
        
        Mottar uredusert stønad.
        
        Inntekten i ${YearMonth.now().minusMonths(7).tilNorskFormat()} er 12 000 kroner. Bruker har inntekt over 1/2 G denne måneden og alle månedene etter dette.
        Stønaden beregnes på nytt fra måneden etter inntekten oversteg 1/2 G.
        """.trimIndent()
}
