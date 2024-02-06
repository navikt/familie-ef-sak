package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.opplysninger.personopplysninger.logger
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import no.nav.familie.kontrakter.felles.ef.StønadType
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
    private val vedtakService: VedtakService,
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
            false -> oppgaverForOpprettelseRepository.insert(OppgaverForOpprettelse(behandlingId, nyeOppgaver))
        }
    }

    fun hentOppgaverForOpprettelseEllerNull(behandlingId: UUID): OppgaverForOpprettelse? {
        return oppgaverForOpprettelseRepository.findByIdOrNull(behandlingId)
    }

    fun hentOppgavetyperSomKanOpprettes(behandlingId: UUID): List<OppgaveForOpprettelseType> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        if (saksbehandling.stønadstype != StønadType.OVERGANGSSTØNAD) {
            return emptyList()
        }

        val vedtak = vedtakService.hentVedtak(behandlingId)
        val kanOppretteInntektskontroll = if (vedtak.resultatType == ResultatType.AVSLÅ &&
            vedtak.avslåÅrsak == AvslagÅrsak.MINDRE_INNTEKTSENDRINGER
        ) {
            kanOppretteInntektsoppgaveForSisteIverksatteBehandling(behandlingId)
        } else if (vedtak.resultatType == ResultatType.INNVILGE) {
            val tilkjentYtelse = tilkjentYtelseService.hentForBehandlingEllerNull(behandlingId)
            kanOppretteOppgaveForInntektskontrollFremITid(tilkjentYtelse)
        } else {
            false
        }
        return if (kanOppretteInntektskontroll) listOf(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID) else emptyList()
    }

    private fun kanOppretteInntektsoppgaveForSisteIverksatteBehandling(behandlingId: UUID): Boolean {
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(behandlingId)
        logger.info("Siste iverksatte behandling: " + sisteIverksatteBehandling?.let { sisteIverksatteBehandling.id })
        return sisteIverksatteBehandling?.let {
            val sisteTilkjentYtelse = tilkjentYtelseService.hentForBehandlingEllerNull(sisteIverksatteBehandling.id)
            logger.info("Sjekker om oppgave kan opprettes ift siste tilkjente ytelse")
            kanOppretteOppgaveForInntektskontrollFremITid(sisteTilkjentYtelse)
        } ?: false
    }

    fun initialVerdierForOppgaverSomSkalOpprettes(behandlingId: UUID) =
        hentOppgavetyperSomKanOpprettes(behandlingId)

    private fun kanOppretteOppgaveForInntektskontrollFremITid(
        tilkjentYtelse: TilkjentYtelse?,
    ): Boolean {
        if (tilkjentYtelse == null) return false

        val harUtbetalingEtterDetNesteÅret = tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.stønadTom > LocalDate.now().plusYears(1) }
            .any { it.beløp > 0 }
        logger.info("Bruker har andeler > 1 år frem i tid")
        return harUtbetalingEtterDetNesteÅret
    }

    fun slettOppgaverForOpprettelse(behandlingId: UUID) {
        oppgaverForOpprettelseRepository.deleteById(behandlingId)
    }
}
