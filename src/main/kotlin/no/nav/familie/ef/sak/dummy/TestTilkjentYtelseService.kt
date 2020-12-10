package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestTilkjentYtelseService(private val behandlingService: BehandlingService,
                                private val fagsakService: FagsakService,
                                private val tilkjentYtelseService: TilkjentYtelseService) {

    @Transactional
    fun lagreTilkjentYtelseOgIverksettUtbetaling(tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): TilkjentYtelse {
        val fagsak = fagsakService.hentEllerOpprettFagsak(tilkjentYtelseTestDTO.nyTilkjentYtelse.personident,
                                                          tilkjentYtelseTestDTO.stønadstype)

        val behandling = behandlingService.opprettBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                             fagsakId = fagsak.id)

        tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseTestDTO.nyTilkjentYtelse.tilDto()
                                                            .copy(behandlingId = behandling.id))
        return tilkjentYtelseService.oppdaterMedUtbetalingsoppdrag(behandling)
    }
}