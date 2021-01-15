package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class DistribuerVedtaksbrevSteg(private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandling: Behandling, data: Void?) {
        // TODO: Implementer dette
        logger.info("Distribuer vedtaksbrev [${behandling.id}]")
        ferdigstillBehandling(behandling)
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }


    override fun stegType(): StegType {
        return StegType.DISTRIBUER_VEDTAKSBREV
    }
}