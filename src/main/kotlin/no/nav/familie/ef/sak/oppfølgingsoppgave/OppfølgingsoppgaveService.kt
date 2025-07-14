package no.nav.familie.ef.sak.oppfølgingsoppgave

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseDto
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseRepository
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingDto
import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingRepository
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevRequest
import no.nav.familie.ef.sak.brev.Brevmal
import no.nav.familie.ef.sak.brev.BrevmottakereService
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.Flettefelter
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev.AutomatiskBrevDto
import no.nav.familie.ef.sak.oppfølgingsoppgave.automatiskBrev.AutomatiskBrevRepository
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.AutomatiskBrev
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForFerdigstilling
import no.nav.familie.ef.sak.oppfølgingsoppgave.domain.OppgaverForOpprettelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
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
    private val automatiskBrevRepository: AutomatiskBrevRepository,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val vedtakService: VedtakService,
    private val iverksettClient: IverksettClient,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevClient: BrevClient,
    private val frittståendeBrevService: FrittståendeBrevService,
    private val personopplysningerService: PersonopplysningerService,
    private val brevmottakereService: BrevmottakereService,
    private val brevsignaturService: BrevsignaturService,
    private val fagsakService: FagsakService,
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

        val oppgavetyperSomKanOpprettes = hentOppgavetyperSomKanOpprettesForOvergangsstønad(behandlingId)
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
        automatiskBrev: List<Brevmal>,
    ) {
        automatiskBrevRepository.deleteByBehandlingId(behandlingId)
        automatiskBrevRepository.insert(AutomatiskBrev(behandlingId, automatiskBrev))
    }

    fun hentOppgaverForOpprettelse(
        behandlingid: UUID,
    ): OppgaverForOpprettelseDto {
        val lagretFremleggsoppgave = hentOppgaverForOpprettelseEllerNull(behandlingid)
        val oppgavetyperSomKanOpprettes = hentOppgavetyperSomKanOpprettesForOvergangsstønad(behandlingid)

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

    fun hentOppgavetyperSomKanOpprettesForOvergangsstønad(behandlingId: UUID): List<OppgaveForOpprettelseType> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        if (saksbehandling.stønadstype == StønadType.SKOLEPENGER) {
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

        oppgavetyperSomKanOpprettes.add(OppgaveForOpprettelseType.INNTEKTSKONTROLL_SELVSTENDIG_NÆRINGSDRIVENDE)

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
        val automatiskBrev = hentAutomatiskBrevEllerNull(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(saksbehandling.fagsakId)
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val personNavn = personopplysningerService.hentGjeldeneNavn(listOf(personIdent)).getValue(personIdent)
        val brevmottakere = brevmottakereService.hentBrevmottakere(behandlingId)
        val signatur = brevsignaturService.lagSaksbehandlerSignatur(fagsak.hentAktivIdent(), VedtakErUtenBeslutter(true))

        if (automatiskBrev != null) {
            automatiskBrev.brevSomSkalSendes.forEach {
                val html =
                    brevClient
                        .genererHtml(
                            brevmal = it.apiNavn,
                            saksbehandlersignatur = signatur.navn,
                            saksbehandlerBrevrequest = objectMapper.valueToTree(BrevRequest(Flettefelter(navn = listOf(personNavn), fodselsnummer = listOf(personIdent)))),
                            skjulBeslutterSignatur = signatur.skjulBeslutter,
                            saksbehandlerEnhet = signatur.enhet,
                        ).replace(VedtaksbrevService.BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())

                val fil = familieDokumentClient.genererPdfFraHtml(html)

                val brevDto = frittståendeBrevService.lagFrittståendeBrevDto(saksbehandling, it.tittel, fil, brevmottakere = brevmottakere)

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

    fun hentOppgaverForOpprettelseEllerNull(behandlingId: UUID): OppgaverForOpprettelse? = oppgaverForOpprettelseRepository.findByIdOrNull(behandlingId)

    fun hentOppgaverForFerdigstillingEllerNull(behandlingId: UUID): OppgaverForFerdigstilling? = oppgaverForFerdigstillingRepository.findByIdOrNull(behandlingId)

    fun hentAutomatiskBrevEllerNull(behandlingId: UUID): AutomatiskBrev? = automatiskBrevRepository.findByIdOrNull(behandlingId)
}
