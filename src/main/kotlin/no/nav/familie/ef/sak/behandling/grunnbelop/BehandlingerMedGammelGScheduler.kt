package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Profile("!integrasjonstest")
@Service
class BehandlingerMedGammelGScheduler(
    val taskService: TaskService,
) {
    @Scheduled(cron = "\${FINN_BEHANDLINGER_MED_GAMMEL_G_CRON_EXPRESSION}")
    @Transactional
    fun opprettTaskFinnBehandlingerMedGammelG() {
        if (LeaderClient.isLeader() == true) {
            val finnesTask =
                taskService.finnTaskMedPayloadOgType(YearMonth.now().toString(), FinnBehandlingerMedGammelGTask.TYPE)
            if (finnesTask == null) {
                val task =
                    Task(
                        type = FinnBehandlingerMedGammelGTask.TYPE,
                        payload = YearMonth.now().toString(),
                    )
                taskService.save(task)
            }
        }
    }
}
