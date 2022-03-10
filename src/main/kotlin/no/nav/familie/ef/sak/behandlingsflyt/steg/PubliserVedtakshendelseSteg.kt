package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.iverksett.IverksettClient
import org.springframework.stereotype.Service

@Service
class PubliserVedtakshendelseSteg(private val iverksettClient: IverksettClient) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        iverksettClient.publiserVedtakshendelse(saksbehandling.id)
    }

    override fun stegType(): StegType {
        return StegType.PUBLISER_VEDTAKSHENDELSE
    }

}