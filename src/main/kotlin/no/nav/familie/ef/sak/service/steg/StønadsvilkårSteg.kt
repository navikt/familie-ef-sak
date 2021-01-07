package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import org.springframework.stereotype.Service

@Service
class StønadsvilkårSteg : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.type == BehandlingType.TEKNISK_OPPHØR) return
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERE_STØNAD
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        // gjør ikke noe
    }

}
