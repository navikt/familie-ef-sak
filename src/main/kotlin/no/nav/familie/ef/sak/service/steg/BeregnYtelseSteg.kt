package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import org.springframework.stereotype.Service

@Service
class BeregnYtelseSteg(private val tilkjentYtelseService: TilkjentYtelseService) : BehandlingSteg<TilkjentYtelseDTO> {

    override fun validerSteg(behandling: Behandling) {

    }

    override fun stegType(): StegType {
        return StegType.BEREGNE_YTELSE
    }

    override fun utførSteg(behandling: Behandling, data: TilkjentYtelseDTO) {
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandling.id)
        tilkjentYtelseService.opprettTilkjentYtelse(data)
    }

}