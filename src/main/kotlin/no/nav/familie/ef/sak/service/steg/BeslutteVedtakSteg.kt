package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import org.springframework.stereotype.Service

@Service
class BeslutteVedtakSteg : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {

    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {

    }

}