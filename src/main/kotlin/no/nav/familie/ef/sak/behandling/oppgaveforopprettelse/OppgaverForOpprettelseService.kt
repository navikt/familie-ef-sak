package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
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
    private val featureToggleService: FeatureToggleService,
) {
    @Transactional
    fun opprettEllerErstatt(
        behandlingId: UUID,
        nyeOppgaver: List<OppgaveForOpprettelseType>,
        årForInntektskontrollSelvstendigNæringsdrivende: Int? = null,
    ) {
        val oppgavetyperSomKanOpprettes = hentOppgavetyperSomKanOpprettes(behandlingId)
        if (oppgavetyperSomKanOpprettes.isEmpty()) {
            oppgaverForOpprettelseRepository.deleteById(behandlingId)
            return
        }
        feilHvisIkke(oppgavetyperSomKanOpprettes.containsAll(nyeOppgaver)) {
            "behandlingId=$behandlingId prøver å opprette $nyeOppgaver $oppgavetyperSomKanOpprettes"
        }
        when (oppgaverForOpprettelseRepository.existsById(behandlingId)) {
            true -> oppgaverForOpprettelseRepository.update(OppgaverForOpprettelse(behandlingId, nyeOppgaver, årForInntektskontrollSelvstendigNæringsdrivende))
            false -> oppgaverForOpprettelseRepository.insert(OppgaverForOpprettelse(behandlingId, nyeOppgaver, årForInntektskontrollSelvstendigNæringsdrivende))
        }
    }

    fun hentOppgaverForOpprettelseEllerNull(behandlingId: UUID): OppgaverForOpprettelse? = oppgaverForOpprettelseRepository.findByIdOrNull(behandlingId)

    fun hentOppgavetyperSomKanOpprettes(behandlingId: UUID): List<OppgaveForOpprettelseType> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        if (saksbehandling.stønadstype != StønadType.OVERGANGSSTØNAD) {
            return emptyList()
        }
        val vedtak = vedtakService.hentVedtak(behandlingId)
        val tilkjentYtelse =
            when {
                vedtak.resultatType == ResultatType.AVSLÅ && vedtak.avslåÅrsak == AvslagÅrsak.MINDRE_INNTEKTSENDRINGER ->
                    hentSisteTilkjentYtelse(saksbehandling.fagsakId)
                vedtak.resultatType == ResultatType.INNVILGE ->
                    tilkjentYtelseService.hentForBehandlingEllerNull(behandlingId)
                else -> null
            }

        val oppgavetyperSomKanOpprettes = mutableListOf<OppgaveForOpprettelseType>()

        val toggleSkalViseOppgavetypeKontrollInntektAvSelvstendigNæringsdrivende = featureToggleService.isEnabled(Toggle.FRONTEND_VIS_MARKERE_GODKJENNE_OPPGAVE_MODAL)

        if (toggleSkalViseOppgavetypeKontrollInntektAvSelvstendigNæringsdrivende) {
            oppgavetyperSomKanOpprettes.add(OppgaveForOpprettelseType.INNTEKTSKONTROLL_SELVSTENDIG_NÆRINGSDRIVENDE)
        }

        if (kanOppretteOppgaveForInntektskontrollFremITid(tilkjentYtelse)) {
            oppgavetyperSomKanOpprettes.add(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)
        }

        return oppgavetyperSomKanOpprettes
    }

    private fun hentSisteTilkjentYtelse(fagsakId: UUID): TilkjentYtelse? {
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
        return sisteIverksatteBehandling?.let {
            tilkjentYtelseService.hentForBehandlingEllerNull(sisteIverksatteBehandling.id)
        }
    }

    fun initialVerdierForOppgaverSomSkalOpprettes(behandlingId: UUID) = hentOppgavetyperSomKanOpprettes(behandlingId)

    private fun kanOppretteOppgaveForInntektskontrollFremITid(
        tilkjentYtelse: TilkjentYtelse?,
    ): Boolean {
        if (tilkjentYtelse == null) return false

        val harUtbetalingEtterDetNesteÅret =
            tilkjentYtelse.andelerTilkjentYtelse
                .filter { it.stønadTom > LocalDate.now().plusYears(1) }
                .any { it.beløp > 0 }

        return harUtbetalingEtterDetNesteÅret
    }

    fun slettOppgaverForOpprettelse(behandlingId: UUID) {
        oppgaverForOpprettelseRepository.deleteById(behandlingId)
    }
}
