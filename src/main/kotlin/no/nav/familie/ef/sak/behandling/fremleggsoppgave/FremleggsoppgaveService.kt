package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FremleggsoppgaveService(
    private val fremleggsoppgaveReporitory: FremleggsoppgaveReporitory,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService
) {

    fun opprettFremleggsoppgave(behandlingId: UUID, opprettFremleggsoppgave: Boolean) {
        fremleggsoppgaveReporitory.insert(Fremleggsoppgave(behandlingId= behandlingId, opprettFremleggsoppgave = opprettFremleggsoppgave))
    }

    fun hentFremleggsoppgave(behandlingId: UUID): Fremleggsoppgave {
        return fremleggsoppgaveReporitory.findByBehandlingId(behandlingId)
    }

    fun kanOpprettes(behandlingId: UUID): Boolean {

        val behandling = behandlingService.hentBehandling(behandlingId)
        val vedtaksresultat = behandling.resultat
        val behandlingstype = behandling.type

        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val sisteAndelMedNullbeløp = tilkjentYtelse.andelerTilkjentYtelse.sortedBy { it.stønadTom }.last().beløp > 0

        return vedtaksresultat == BehandlingResultat.INNVILGET
            && behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING
            && sisteAndelMedNullbeløp
    }
}