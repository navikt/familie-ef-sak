package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FremleggsoppgaveService(
    private val fremleggsoppgaveReporitory: FremleggsoppgaveReporitory,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService
) {

    @Transactional
    fun opprettEllerErstattFremleggsoppgave(behandlingId: UUID, opprettFremleggsoppgave: Boolean) {
        when (fremleggsoppgaveReporitory.existsById(behandlingId)) {
            true -> fremleggsoppgaveReporitory.update(
                Fremleggsoppgave(behandlingId = behandlingId, opprettFremleggsoppgave = opprettFremleggsoppgave)
            )
            false -> fremleggsoppgaveReporitory.insert(
                Fremleggsoppgave(behandlingId = behandlingId, opprettFremleggsoppgave = opprettFremleggsoppgave)
            )
        }
    }

    fun hentFremleggsoppgave(behandlingId: UUID): Fremleggsoppgave? {
        return fremleggsoppgaveReporitory.findByIdOrNull(behandlingId)?.let { it }
    }

    fun kanOpprettes(behandlingId: UUID): Boolean {

        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingstype = behandling.type

        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val sisteAndelMedNullbeløp = tilkjentYtelse.andelerTilkjentYtelse.sortedBy { it.stønadTom }.last().beløp > 0

        val resultatType = vedtakService.hentVedtak(behandlingId).resultatType
        return resultatType == ResultatType.INNVILGE
            && behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING
            && sisteAndelMedNullbeløp
    }
}