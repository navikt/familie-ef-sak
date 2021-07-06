package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.task.PubliserVedtakshendelseTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class FerdigstillBehandlingSteg(private val behandlingService: BehandlingService,
                                private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandling: Behandling, data: Void?) {
        logger.info("Ferdigstiller behandling [${behandling.id}]")
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)

        if (behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING || behandling.type == BehandlingType.REVURDERING) {
            behandlingService.oppdaterResultatPåBehandling(behandling.id)
            taskRepository.save(PubliserVedtakshendelseTask.opprettTask(behandling.id))
            taskRepository.save(BehandlingsstatistikkTask.opprettFerdigTask(behandlingId = behandling.id))
        } else if (behandling.type == BehandlingType.BLANKETT || behandling.type == BehandlingType.TEKNISK_OPPHØR) {
            //ignore
        } else {
            error("Har ikke lagt inn håndtering av type=${behandling.type} i ferdigstilling")
        }
    }


    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }
}