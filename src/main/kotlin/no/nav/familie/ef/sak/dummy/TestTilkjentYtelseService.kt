package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestTilkjentYtelseService(private val behandlingService: BehandlingService,
                                private val fagsakService: FagsakService,
                                private val oppdragClient: OppdragClient,
                                private val tilkjentYtelseRepository: TilkjentYtelseRepository) {

    @Transactional
    fun lagreTilkjentYtelseOgIverksettUtbetaling(tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): TilkjentYtelse {
        val fagsak = fagsakService.hentEllerOpprettFagsak(tilkjentYtelseTestDTO.nyTilkjentYtelse.personident,
                                                          Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                             fagsakId = fagsak.id)
        val eksternFagsakId = fagsakService.hentEksternId(behandling.fagsakId)

        val nyTilkjentYtelse = tilkjentYtelseTestDTO.nyTilkjentYtelse.copy(behandlingId = behandling.id)
        val forrigeTilkjentYtelse = tilkjentYtelseTestDTO.forrigeTilkjentYtelse

        val nyTilkjentYtelseMedEksternId = TilkjentYtelseMedMetaData(nyTilkjentYtelse,
                                                                     eksternBehandlingId = behandling.eksternId.id,
                                                                     eksternFagsakId = eksternFagsakId)
        val tilkjentYtelseMedUtbetalingsoppdrag =
                UtbetalingsoppdragGenerator
                        .lagTilkjentYtelseMedUtbetalingsoppdrag(nyTilkjentYtelseMedMetaData = nyTilkjentYtelseMedEksternId,
                                                                forrigeTilkjentYtelse = forrigeTilkjentYtelse)

        tilkjentYtelseRepository.insert(tilkjentYtelseMedUtbetalingsoppdrag)
        oppdragClient.iverksettOppdrag(tilkjentYtelseMedUtbetalingsoppdrag.utbetalingsoppdrag!!)

        return tilkjentYtelseMedUtbetalingsoppdrag
    }
}