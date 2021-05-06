package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtaksbrev.IverksettClient
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SimuleringService(private val iverksettClient: IverksettClient,
                        private val behandlingService: BehandlingService,
                        private val fagsakService: FagsakService,
                        private val tilkjentYtelseService: TilkjentYtelseService) {


    fun simulerForBehandling(behandlingId: UUID): DetaljertSimuleringResultat {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val tilkjentYtelseMedMedtadata =
                tilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                                                       eksternBehandlingId = behandling.eksternId.id,
                                                       stønadstype = fagsak.stønadstype,
                                                       eksternFagsakId = fagsak.eksternId.id

                )
        val forrigeTilkjentYtelse = tilkjentYtelseService.finnSisteTilkjentYtelse(fagsakId = behandling.fagsakId)?.tilIverksett()

        return iverksettClient.simuler(SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseMedMedtadata,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse

        ))
    }


}