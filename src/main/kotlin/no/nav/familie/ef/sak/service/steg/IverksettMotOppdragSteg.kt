package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdragSteg (private val tilkjentYtelseService: TilkjentYtelseService) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        // TODO
    }

    override fun stegType(): StegType {
       return StegType.IVERKSETT_MOT_OPPDRAG
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
       tilkjentYtelseService.opprettTilkjentYtelse(behandling)
    }
}