package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.PubliserVedtakshendelseTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FerdigstillBehandlingSteg(
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepository
) : BehandlingSteg<Void?> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        logger.info("Ferdigstiller behandling [${saksbehandling.id}]")
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.FERDIGSTILT)

        when (saksbehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING, BehandlingType.REVURDERING -> {
                taskRepository.save(PubliserVedtakshendelseTask.opprettTask(saksbehandling.id))
                if (!saksbehandling.erMigrering && !saksbehandling.erMaskinellOmregning) {
                    taskRepository.save(BehandlingsstatistikkTask.opprettFerdigTask(behandlingId = saksbehandling.id))
                }
            }
            BehandlingType.BLANKETT, BehandlingType.TEKNISK_OPPHØR -> {
                // ignore
            }
        }
    }

    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }
}
