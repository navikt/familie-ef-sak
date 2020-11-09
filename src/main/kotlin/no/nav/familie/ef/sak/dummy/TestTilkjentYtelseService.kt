package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.YtelseType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.tilYtelseType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TestTilkjentYtelseService(private val behandlingService: BehandlingService,
                                private val fagsakService: FagsakService,
                                private val tilkjentYtelseService: TilkjentYtelseService) {


    @Transactional
    fun iverksettBehandling(dummyIverksettingDTO: DummyIverksettingDTO): UUID {

        val fagsakDto = fagsakService.hentEllerOpprettFagsak(personIdent = dummyIverksettingDTO.personIdent,
                                                             stønadstype = dummyIverksettingDTO.stønadstype)
        val behandling = behandlingService.opprettBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                             fagsakId = fagsakDto.id)

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.IVERKSETTER_VEDTAK)

        val andelTilkjentYtelseDTO = AndelTilkjentYtelseDTO(beløp = dummyIverksettingDTO.beløp,
                                                            stønadFom = dummyIverksettingDTO.stønadFom,
                                                            stønadTom = dummyIverksettingDTO.stønadTom,
                                                            type = dummyIverksettingDTO.stønadstype.tilYtelseType(),
                                                            personIdent = dummyIverksettingDTO.personIdent)
        val tilkjentYtelseDTO = TilkjentYtelseDTO(søker = fagsakDto.personIdent,
                                                  behandlingId = behandling.id,
                                                  andelerTilkjentYtelse = listOf(andelTilkjentYtelseDTO))

        val tilkjentYtelseId = tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseDTO = tilkjentYtelseDTO)

        tilkjentYtelseService.iverksettUtbetalingsoppdrag(ytelseId = tilkjentYtelseId)

        return tilkjentYtelseId
    }
}