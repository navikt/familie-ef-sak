package no.nav.familie.ef.sak.oppfølgingsoppgave

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseDto
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseRepository
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingDto
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingRepository
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForFerdigstilling
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForOpprettelse
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OppfølgingsoppgaveService(
    private val oppgaverForFerdigstillingRepository: OppgaverForFerdigstillingRepository,
    private val oppgaverForOpprettelseRepository: OppgaverForOpprettelseRepository,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
    private val featureToggleService: FeatureToggleService,
) {
    @Transactional
    fun lagreOppgaveIderForFerdigstilling(
        behandlingId: UUID,
        oppgaveIder: List<Long>,
    ) {
        oppgaverForFerdigstillingRepository.deleteByBehandlingId(behandlingId)
        oppgaverForFerdigstillingRepository.insert(
            OppgaverForFerdigstilling(behandlingId, oppgaveIder),
        )
    }

    @Transactional
    fun lagreOppgaverForOpprettelse(
        behandlingId: UUID,
        data: SendTilBeslutterDto,
    ) {
        val nyeOppgaver = data.oppgavetyperSomSkalOpprettes
        val årForInntektskontrollSelvstendigNæringsdrivende = data.årForInntektskontrollSelvstendigNæringsdrivende

        val oppgavetyperSomKanOpprettes = hentOppgavetyperSomKanOpprettes(behandlingId)
        if (oppgavetyperSomKanOpprettes.isEmpty()) {
            oppgaverForOpprettelseRepository.deleteById(behandlingId)
            return
        }
        feilHvisIkke(oppgavetyperSomKanOpprettes.containsAll(nyeOppgaver)) {
            "behandlingId=$behandlingId prøver å opprette $nyeOppgaver $oppgavetyperSomKanOpprettes"
        }
        oppgaverForOpprettelseRepository.deleteByBehandlingId(behandlingId)
        oppgaverForOpprettelseRepository.insert(OppgaverForOpprettelse(behandlingId, nyeOppgaver, årForInntektskontrollSelvstendigNæringsdrivende))
    }

    fun hentOppgaverForOpprettelse(
        behandlingid: UUID,
    ): OppgaverForOpprettelseDto {
        val lagretFremleggsoppgave = hentOppgaverForOpprettelseEllerNull(behandlingid)
        val oppgavetyperSomKanOpprettes = hentOppgavetyperSomKanOpprettes(behandlingid)

        return (
            OppgaverForOpprettelseDto(
                oppgavetyperSomKanOpprettes = oppgavetyperSomKanOpprettes,
                oppgavetyperSomSkalOpprettes =
                    lagretFremleggsoppgave?.oppgavetyper
                        ?: emptyList(),
            )
        )
    }

    fun hentOppgaverForFerdigstilling(
        behandlingid: UUID,
    ): OppgaverForFerdigstillingDto {
        val lagretFremleggsoppgaveIder = hentOppgaverForFerdigstillingEllerNull(behandlingid)

        return(
            OppgaverForFerdigstillingDto(
                behandlingId = behandlingid,
                oppgaveIder =
                    lagretFremleggsoppgaveIder?.fremleggsoppgaveIderSomSkalFerdigstilles
                        ?: emptyList(),
            )
        )
    }

    fun hentOppgavetyperSomKanOpprettes(behandlingId: UUID): List<OppgaveForOpprettelseType> {
        val toggleSkalViseOppgavetypeKontrollInntektAvSelvstendigNæringsdrivende = featureToggleService.isEnabled(Toggle.FRONTEND_VIS_MARKERE_GODKJENNE_OPPGAVE_MODAL)

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

        if (kanOppretteOppgaveForInntektskontrollFremITid(tilkjentYtelse)) {
            oppgavetyperSomKanOpprettes.add(OppgaveForOpprettelseType.INNTEKTSKONTROLL_1_ÅR_FREM_I_TID)
        }

        if (toggleSkalViseOppgavetypeKontrollInntektAvSelvstendigNæringsdrivende) {
            oppgavetyperSomKanOpprettes.add(OppgaveForOpprettelseType.INNTEKTSKONTROLL_SELVSTENDIG_NÆRINGSDRIVENDE)
        }

        return oppgavetyperSomKanOpprettes
    }

    private fun hentSisteTilkjentYtelse(fagsakId: UUID): TilkjentYtelse? {
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
        return sisteIverksatteBehandling?.let {
            tilkjentYtelseService.hentForBehandlingEllerNull(sisteIverksatteBehandling.id)
        }
    }

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

    fun slettOppfølgingsoppgave(behandlingId: UUID) {
        oppgaverForOpprettelseRepository.deleteById(behandlingId)
        oppgaverForFerdigstillingRepository.deleteById(behandlingId)
    }

    fun hentOppgaverForOpprettelseEllerNull(behandlingId: UUID): OppgaverForOpprettelse? = oppgaverForOpprettelseRepository.findByIdOrNull(behandlingId)

    fun hentOppgaverForFerdigstillingEllerNull(behandlingId: UUID): OppgaverForFerdigstilling? = oppgaverForFerdigstillingRepository.findByIdOrNull(behandlingId)
}
