package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.service.BehandlingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class FerdigstillBehandlingSteg(private val behandlingService: BehandlingService) : BehandlingSteg<Void?> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun utførSteg(behandling: Behandling, data: Void?) {
        logger.info("Ferdigstiller behandling [${behandling.id}]")
        behandlingService.oppdaterStatusPåBehandling(behandling, BehandlingStatus.FERDIGSTILT)
    }


    override fun stegType(): StegType {
        return StegType.FERDIGSTILLE_BEHANDLING
    }
}