package no.nav.familie.ef.sak.patchstatistikk

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/patch-statistikk")
@Unprotected
class PatchStatistikkController(private val taskService: TaskService, private val behandlingRepository: BehandlingRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/henlagt")
    fun patchHenlagteBehandlingerTilStatistikk(@RequestBody liveRun: LiveRun) {
        val behandlinger: List<Behandling> = finnBehandlingerSomErHenlagt().filterNot { it.erMigrering() }
        logger.info("Skal patche ${behandlinger.size} henlagte behandlinger til datavarehus med ferdigstilt/henlagt-resultat")

        val behandlingsstatistikkPayloads = behandlinger.map {
            BehandlingsstatistikkTask.opprettHenlagtTask(
                behandlingId = it.id,
                hendelseTidspunkt = it.sporbar.endret.endretTid,
                gjeldendeSaksbehandler = it.sporbar.endret.endretAv
            )
        }
        if (liveRun.skalPersistere) {
            logger.info("Persisterer behandlingsstatistikk-tasks for henlagte behandlinger")
            behandlingsstatistikkPayloads.forEach {
                taskService.save(it)
            }
        }
    }

    private fun finnBehandlingerSomErHenlagt(): List<Behandling> {
        return behandlingRepository.finnHenlagteBehandlingerSomSkalOversendesTilDatavarehus()
    }

    data class LiveRun(val skalPersistere: Boolean)
}
