package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.domain.Behandling
import org.springframework.stereotype.Service

@Service
class PubliserVedtakshendelseSteg(private val iverksettClient: IverksettClient) : BehandlingSteg<Void?> {

    override fun utførSteg(behandling: Behandling, data: Void?) {
        iverksettClient.publiserVedtakshendelse(behandling.id)
    }

    override fun stegType(): StegType {
        return StegType.PUBLISER_VEDTAKSHENDELSE
    }

}