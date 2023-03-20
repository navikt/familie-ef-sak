package no.nav.familie.ef.sak.iverksett.arbeidsoppfolging

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Properties
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/arbeidsoppfolging-patch"])
@Unprotected
class PatchArbeidsoppfølgingController(
    val behandlingRepository: BehandlingRepository,
    val taskService: TaskService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun patchSendArbeidsoppfølgingInfoForPersonerMedAktivOvergangsstønad(@RequestParam liveRun: Boolean = false) {
        logger.info("Starter patch for sending av aktive iverksatte behandlinger til arbeidsoppfølging (liverun=$liveRun)")
        val behandlingIds = behandlingRepository.finnBehandlingerForPersonerMedAktivStønad(StønadType.OVERGANGSSTØNAD)
        logger.info("Antall aktive behandlinger for overgangsstønad funnet: ${behandlingIds.size}")

        val tasks = behandlingIds.map { PatchSendTilArbeidsoppfølgingTask.opprettTask(it) }
        if (liveRun) {
            taskService.saveAll(tasks)
        }
    }
}

@Service
@TaskStepBeskrivelse(
    taskStepType = PatchSendTilArbeidsoppfølgingTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Patch for å sende alle med aktiv overgangsstønad til arbeidsoppfølging"
)
class PatchSendTilArbeidsoppfølgingTask(val iverksettClient: IverksettClient) : AsyncTaskStep {

    override fun doTask(task: Task) {
        iverksettClient.sendTilArbeidsoppfølging(task.payload)
    }

    companion object {

        const val TYPE = "patchSendTilArbeidsoppfølging"

        fun opprettTask(behandlingId: UUID): Task {
            return Task(
                type = OpprettOppgaveForOpprettetBehandlingTask.TYPE,
                payload = behandlingId.toString(),
                properties = Properties().apply {
                    this["behandlingId"] = behandlingId.toString()
                }
            )
        }
    }
}
