package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.BehandlingService
import org.springframework.stereotype.Service

@Service
class VilkårSteg(private val behandlingService: BehandlingService) : BehandlingSteg<String?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.type == BehandlingType.TEKNISK_OPPHØR) return
    }

    override fun utførSteg(behandling: Behandling, data: String?) {
        // TODO: Søknad og behandling kan kobles sammen her
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.UTREDES)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅR
    }
}