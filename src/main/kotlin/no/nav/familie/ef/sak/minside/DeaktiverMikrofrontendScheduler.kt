package no.nav.familie.ef.sak.minside

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeaktiverMikrofrontendScheduler(
    val taskService: TaskService,
    val fagsakPersonService: FagsakPersonService,
) {
    private val logger = Logg.getLogger(this::class)

    @Scheduled(cron = "\${DEAKTIVER_MIKROFRONTEND_CRON_EXPRESSION}")
    @Transactional
    fun opprettTaskForDeaktiveringAvMikrofrontend() {
        if (LeaderClient.isLeader() == true) {
            logger.info("Starter scheduler for å deaktivere mikrofrontend for brukere")
            val fagsakPersonIder = fagsakPersonService.finnFagsakpersonIderKlarForDeaktiveringAvMikrofrontend()
            logger.info("Fant ${fagsakPersonIder.size} som skal deaktivere mikrofrontend for enslig forsørger")
            fagsakPersonIder.forEach {
                val task = DeaktiverMikrofrontendTask.opprettTask(it)
                taskService.save(task)
            }
        }
    }
}
