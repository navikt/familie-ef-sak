package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.revurdering.AutomatiskRevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.BehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.PayloadBehandleAutomatiskInntektsendringTask
import no.nav.familie.ef.sak.behandling.revurdering.RevurderingService
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.findByIdOrThrow
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
        val behandling = behandling(fagsak, resultat = BehandlingResultat.IKKE_SATT, status = BehandlingStatus.UTREDES)
        behandlingRepository.insert(behandling)
        vilkårHelperService.opprettVilkår(behandling)
        vedtakHelperService.ferdigstillVedtak(vedtak(behandlingId = behandling.id, månedsperiode = Månedsperiode(YearMonth.now().minusMonths(4), YearMonth.now())), behandling, fagsak)

        val payload = PayloadBehandleAutomatiskInntektsendringTask(personIdent, "2025-15")
        val test = BehandleAutomatiskInntektsendringTask.opprettTask(objectMapper.writeValueAsString(payload))

        behandleAutomatiskInntektsendringTask.doTask(test)

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
        assertThat(data.mappeId).isEqualTo(63)
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
    fun `to eksisterende inntektsperioder - sett revurderes fra måneden etter 10 prosent endring`() {
        // Vedtak fra August 2024 -> Juli 2027
        // Inntektsperioder: August 54 534, September 39129
        // Beregnet ny forventet inntekt: 43796
        val innmeldtMånedsinntekt = listOf(54534.36, 39129.14, 36361.58, 37609.86, 41796.68, 43213.59, 44122.90, 44052.95, 43213.59)
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
        assertThat(oppdatertInntekt.size).isEqualTo(3)
    }
}
