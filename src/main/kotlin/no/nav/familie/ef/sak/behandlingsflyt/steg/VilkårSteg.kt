package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import org.springframework.stereotype.Service

@Service
class VilkårSteg(private val behandlingService: BehandlingService) : BehandlingSteg<String?> {

    override fun validerSteg(behandling: Saksbehandling) {
        if (behandling.type == BehandlingType.TEKNISK_OPPHØR) return
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: String?) {
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅR
    }
}