package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.VedtaksbrevService
import no.nav.familie.ef.sak.task.DistribuerVedtaksbrevTask
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class JournalførVedtaksbrevSteg(private val taskRepository: TaskRepository,
                                private val vedtaksbrevService: VedtaksbrevService,
                                private val behandlingService: BehandlingService) : BehandlingSteg<Void?> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandling: Behandling, data: Void?) {
        val journalpostId = vedtaksbrevService.journalførVedtaksbrev(behandling.id)
        behandlingService.leggTilBehandlingsjournalpost(journalpostId
                                                        ?: error("Feil ved journalføring av vedtaksbrev"),
                                                        Journalposttype.U,
                                                        behandling.id)
        logger.info("Journalfør vedtaksbrev behandling=[${behandling.id}] fullført med journalpost=[$journalpostId]")
        lagDistribuerVedtaksbrevTask(behandling, journalpostId)
    }

    private fun lagDistribuerVedtaksbrevTask(behandling: Behandling, journalpostId: String) {
        taskRepository.save(DistribuerVedtaksbrevTask.opprettTask(behandling, journalpostId))
    }


    override fun stegType(): StegType {
        return StegType.JOURNALFØR_VEDTAKSBREV
    }


}