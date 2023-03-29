package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OppgaverForOpprettelseService(
    private val oppgaverForOpprettelseRepository: OppgaverForOpprettelseRepository,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {

    @Transactional
    fun opprettEllerErstattFremleggsoppgave(oppgaverForOpprettelse: OppgaverForOpprettelse) {
        when (this.oppgaverForOpprettelseRepository.existsById(oppgaverForOpprettelse.behandlingId)) {
            true -> this.oppgaverForOpprettelseRepository.update(oppgaverForOpprettelse)
            false -> this.oppgaverForOpprettelseRepository.insert(oppgaverForOpprettelse)
        }
    }

    fun hentFremleggsoppgave(behandlingId: UUID): OppgaverForOpprettelse? {
        return oppgaverForOpprettelseRepository.findByIdOrNull(behandlingId)
    }

    fun kanOpprettes(behandlingId: UUID): Boolean {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingstype = behandling.type

        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val sisteAndel = tilkjentYtelse.andelerTilkjentYtelse.sortedBy { it.stønadTom }.last()
        val sisteAndelMedBeløp = sisteAndel.beløp > 0
        val sisteAndel1årFremITid = sisteAndel.stønadTom.minusYears(1) > LocalDate.now()

        return behandlingstype == BehandlingType.FØRSTEGANGSBEHANDLING &&
            sisteAndelMedBeløp &&
            sisteAndel1årFremITid
    }
}
