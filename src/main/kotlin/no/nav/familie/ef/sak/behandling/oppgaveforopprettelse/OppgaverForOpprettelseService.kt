package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
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
    fun opprettEllerErstatt(behandlingId: UUID, nyeOppgaver: List<OppgaveForOpprettelseType>) {
        val oppgavetyperSomKanOpprettes = hentOppgavetyperSomKanOpprettes(behandlingId)
        if (oppgavetyperSomKanOpprettes.isEmpty()) {
            oppgaverForOpprettelseRepository.deleteById(behandlingId)
            return
        }
        feilHvisIkke(oppgavetyperSomKanOpprettes.containsAll(nyeOppgaver)) {
            "behandlingId=$behandlingId prøver å opprette $nyeOppgaver $oppgavetyperSomKanOpprettes"
        }
        when (oppgaverForOpprettelseRepository.existsById(behandlingId)) {
            true -> oppgaverForOpprettelseRepository.update(OppgaverForOpprettelse(behandlingId, nyeOppgaver))
            false ->oppgaverForOpprettelseRepository.insert(OppgaverForOpprettelse(behandlingId, nyeOppgaver))
        }
    }

    fun hentOppgaverForOpprettelseEllerNull(behandlingId: UUID): OppgaverForOpprettelse? {
        return oppgaverForOpprettelseRepository.findByIdOrNull(behandlingId)
    }

    fun hentOppgavetyperSomKanOpprettes(behandlingId: UUID): List<OppgaveForOpprettelseType> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandlingEllerNull(behandlingId)
        val kanOppretteInntektskontroll = kanOppretteOppgaveForInntektskontrollFremITid(behandling, tilkjentYtelse)

        return if (kanOppretteInntektskontroll) listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID) else emptyList()
    }

    fun initialVerdierForOppgaverSomSkalOpprettes(behandlingId: UUID) = hentOppgavetyperSomKanOpprettes(behandlingId)

    private fun kanOppretteOppgaveForInntektskontrollFremITid(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse?,
    ): Boolean {
        if (tilkjentYtelse == null) return false

        val harUtbetalingEtterDetNesteÅret = tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.stønadTom > LocalDate.now().plusYears(1) }
            .any { it.beløp > 0 }

        return behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING && harUtbetalingEtterDetNesteÅret
    }

    fun slettOppgaverForOpprettelse(behandlingId: UUID) {
        oppgaverForOpprettelseRepository.deleteById(behandlingId)
    }
}
