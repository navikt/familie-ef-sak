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

        // Har ikke mocket inntekt-response, revurderes fra-dato blir derfor satt lik som fradato i forrige behandling
        assertThat(førsteFom).isEqualTo(YearMonth.of(2025, 1))
        assertThat(inntektsperioder?.first()?.inntekt?.toInt()).isEqualTo(420_000)

        val opprettOppgaveTask = taskService.findAll().first { it.type == OpprettOppgaveForOpprettetBehandlingTask.TYPE }
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(opprettOppgaveTask.payload)
        assertThat(data.mappeId).isEqualTo(63)
        assertThat(data.beskrivelse).isEqualTo("Automatisk opprettet revurdering som følge av inntektskontroll")
    }
}
