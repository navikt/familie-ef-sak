package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.VedtaksbrevIverksettingService
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class DistribuerVedtaksbrevSteg(private val taskRepository: TaskRepository,
                                private val vedtaksbrevServiceService: VedtaksbrevIverksettingService) : BehandlingSteg<String> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandling: Behandling, jourpostId: String) {
        logger.info("Distribuer vedtaksbrev journalpost=[$jourpostId] for behandling=[${behandling.id}]")
        val bestillingId = vedtaksbrevServiceService.distribuerVedtaksbrev(behandling.id, jourpostId)
        logger.info("Distribuer vedtaksbrev journalpost=[$jourpostId] for behandling=[${behandling.id}] med bestillingId=[$bestillingId]")
        ferdigstillBehandling(behandling)
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }


    override fun stegType(): StegType {
        return StegType.DISTRIBUER_VEDTAKSBREV
    }
}