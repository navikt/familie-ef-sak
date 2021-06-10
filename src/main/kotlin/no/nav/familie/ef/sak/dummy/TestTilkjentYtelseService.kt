package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.FagsakDto
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TestTilkjentYtelseService(private val behandlingService: BehandlingService,
                                private val fagsakService: FagsakService,
                                private val tilkjentYtelseService: TilkjentYtelseService) {

    @Transactional
    fun lagreTilkjentYtelseOgIverksettUtbetaling(tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): TilkjentYtelse {
        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger(tilkjentYtelseTestDTO.nyTilkjentYtelse.søker,
                                                                         tilkjentYtelseTestDTO.stønadstype)

        val behandling = behandlingService.opprettBehandling(behandlingType = behandlingType(fagsak),
                                                             fagsakId = fagsak.id)

        val tilkjentYtelseDTO = tilkjentYtelseTestDTO.nyTilkjentYtelse.copy(id = UUID.randomUUID(),
                                                                            behandlingId = behandling.id)
        val opprettTilkjentYtelse = tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseDTO)
        tilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(behandling)
        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id,
                                                     status = BehandlingStatus.FERDIGSTILT)
        return opprettTilkjentYtelse
    }

    fun konsistensavstemOppdrag(stønadstype: Stønadstype): KonsistensavstemmingDto {
        val konsistensavstemming = tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(stønadstype = stønadstype,
                                                            datoForAvstemming = LocalDate.now())
        return KonsistensavstemmingDto(StønadType.valueOf(stønadstype.name), konsistensavstemming)
    }

    private fun behandlingType(fagsak: FagsakDto): BehandlingType =
            if (behandlingService.hentBehandlinger(fagsakId = fagsak.id).isEmpty()) {
                BehandlingType.FØRSTEGANGSBEHANDLING
            } else {
                BehandlingType.REVURDERING
            }
}