package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.task.DistribuerVedtaksbrevTask
import no.nav.familie.ef.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class JournalførVedtaksbrevSteg(private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandling: Behandling, data: Void?) {
        // TODO: Implementer dette
        logger.info("Journalfør vedtaksbrev [${behandling.id}]")

        distribuerVedtaksbrev(behandling)
    }

    private fun distribuerVedtaksbrev(behandling: Behandling) {
        taskRepository.save(DistribuerVedtaksbrevTask.opprettTask(behandling))
    }


    override fun stegType(): StegType {
        return StegType.JOURNALFØR_VEDTAKSBREV
    }
}