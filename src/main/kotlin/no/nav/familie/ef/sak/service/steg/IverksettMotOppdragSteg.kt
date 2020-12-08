package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdragSteg (private val tilkjentYtelseService: TilkjentYtelseService) : BehandlingSteg<Void> {

    override fun stegType(): StegType {
        TODO("Not yet implemented")
    }

    override fun utførSteg(behandling: Behandling, data: Void) {
        TODO("Not yet implemented")
    }
}