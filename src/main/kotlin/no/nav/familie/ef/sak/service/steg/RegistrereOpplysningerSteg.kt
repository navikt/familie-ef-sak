package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.service.BehandlingService
import org.springframework.stereotype.Service

@Service
class RegistrereOpplysningerSteg(
        private val behandlingService: BehandlingService
) : BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        // TODO: Søknad og behandling kan kobles sammen her
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.UTREDES)
        return hentNesteSteg(behandling)
    }

    override fun stegType(): StegType {
        return StegType.REGISTRERE_OPPLYSNINGER
    }
}

