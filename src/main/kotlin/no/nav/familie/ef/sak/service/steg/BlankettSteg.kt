package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.domain.Behandling
import org.springframework.stereotype.Service

@Service
class BlankettSteg() : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        TODO("Not yet implemented")
    }

    override fun stegType(): StegType = StegType.JOURNALFØR_BLANKETT


}