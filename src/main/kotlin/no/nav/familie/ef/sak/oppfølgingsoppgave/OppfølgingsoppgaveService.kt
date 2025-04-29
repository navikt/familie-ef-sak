package no.nav.familie.ef.sak.oppfølgingsoppgave

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseDto
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseRepository
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingDto
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingRepository
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev.AutomatiskBrevDto
import no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev.AutomatiskBrevRepository
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.AutomatiskBrev
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForFerdigstilling
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForOpprettelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.leader.LeaderClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class OppfølgingsoppgaveService(
    private val oppgaverForFerdigstillingRepository: OppgaverForFerdigstillingRepository,
    private val oppgaverForOpprettelseRepository: OppgaverForOpprettelseRepository,
    private val automatiskBrevRepository: AutomatiskBrevRepository,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
    private val featureToggleService: FeatureToggleService,
    private val iverksettClient: IverksettClient,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevClient: BrevClient,
    private val frittståendeBrevService: FrittståendeBrevService,
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

    @Transactional
    fun lagreAutomatiskBrev(
        behandlingId: UUID,
        automatiskBrev: List<String>,
    ) {
        automatiskBrevRepository.deleteByBehandlingId(behandlingId)
        automatiskBrevRepository.insert(AutomatiskBrev(behandlingId, automatiskBrev))
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

    fun hentAutomatiskBrev(
        behandlingId: UUID,
    ): AutomatiskBrevDto {
        val automatiskBrev = hentAutomatiskBrevEllerNull(behandlingId)

        return AutomatiskBrevDto(
            behandlingId = behandlingId,
            brevSomSkalSendes = automatiskBrev?.brevSomSkalSendes ?: emptyList(),
        )
    }

    fun sendAutomatiskBrev(
        behandlingId: UUID,
    ) {
        if (LeaderClient.isLeader() == false) return // Er denne nødvendig?

        val automatiskBrev = hentAutomatiskBrevEllerNull(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val personIdent = behandlingService.hentAktivIdent(behandlingId)

        if (automatiskBrev != null) {
            automatiskBrev.brevSomSkalSendes.forEach {
                val brevmal = brevtittelTilBrevmal(it)

                val html =
                    brevClient
                        .genererHtml(
                            brevmal = brevmal,
                            saksbehandlersignatur = "Vedtaksløsningen",
                            saksbehandlerBrevrequest = objectMapper.valueToTree(BrevRequest(Flettefelter(navn = listOf(it), fodselsnummer = listOf(personIdent)))),
                            enhet = "NAV Arbeid og ytelser",
                            skjulBeslutterSignatur = true,
                        ).replace(VedtaksbrevService.BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())

                val fil = familieDokumentClient.genererPdfFraHtml(html)

                val brevDto = frittståendeBrevService.lagFrittståendeBrevDto(saksbehandling, it, fil)

                secureLogger.info("Sender automatisk brev: $brevmal til mottaker: ${brevDto.mottakere}")
                secureLogger.info("---- html ----: $html")
                iverksettClient.sendFrittståendeBrev(frittståendeBrevDto = brevDto)
            }

            automatiskBrevRepository.deleteByBehandlingId(behandlingId)
        }
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

    private fun brevtittelTilBrevmal(brevtittel: String): String { // TODO: utbedre type
        return when (brevtittel) {
            "Varsel om aktivitetsplikt" -> "varselAktivitetsplikt"
            else -> brevtittel
        }
    }

    fun hentOppgaverForOpprettelseEllerNull(behandlingId: UUID): OppgaverForOpprettelse? = oppgaverForOpprettelseRepository.findByIdOrNull(behandlingId)

    fun hentOppgaverForFerdigstillingEllerNull(behandlingId: UUID): OppgaverForFerdigstilling? = oppgaverForFerdigstillingRepository.findByIdOrNull(behandlingId)

    fun hentAutomatiskBrevEllerNull(behandlingId: UUID): AutomatiskBrev? = automatiskBrevRepository.findByIdOrNull(behandlingId)
}

data class BrevRequest(
    val flettefelter: Flettefelter,
)

data class Flettefelter(
    val navn: List<String>? = emptyList(),
    val fodselsnummer: List<String>? = emptyList(),
    val forventetInntekt: List<Int>? = emptyList(),
)
