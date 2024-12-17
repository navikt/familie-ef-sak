package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.amelding.InntektService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.ef.sak.sigrun.SigrunService
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class NæringsinntektKontrollService(
    val oppgaveService: OppgaveService,
    val fagsakService: FagsakService,
    val behandlingService: BehandlingService,
    val vedtakService: VedtakService,
    val sigrunService: SigrunService,
    val inntektService: InntektService,
    val andelsHistorikkService: AndelsHistorikkService,
    val tilkjentYtelseService: TilkjentYtelseService,
    val næringsinntektBrukernotifikasjonService: NæringsinntektBrukernotifikasjonService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private val årstallIFjor = YearMonth.now().year - 1

    fun kontrollerInntektForSelvstendigNæringsdrivende(): List<UUID> {
        if (LeaderClient.isLeader() == false) return emptyList()

        val fagsakIds = mutableListOf<UUID>()
        val oppgaver = hentOppgaverForSelvstendigeTilInntektskontroll()

        val næringsinntektDataForBeregningList = hentNæringsinntektDataForBeregning(oppgaver)

        næringsinntektDataForBeregningList.forEach {
            if (skalKontrolleres(it.tilkjentYtelse, it.fagsak)) {
                if (oppfyllerAktivitetsplikt(it.fjoråretsPersonInntekt, it.fjoråretsNæringsinntekt, it.fagsak)) {
                    if (it.fjoråretsNæringsinntekt >= (it.forventetInntektIFjor * 1.1)) {
                        secureLogger.info("Har 10% høyere næringsinntekt for person: ${it.personIdent} (Næringsinntekt: ${it.forventetInntektIFjor} - ForventetInntekt: ${it.forventetInntektIFjor})")
                        fagsakIds.add(it.fagsak.id)
                        giVarselOmNyVurderingAvInntekt(it.behandlingId, it.personIdent)
                    } else {
                        // Lag notat
                        giBeskjedOmKontrollertInntektVedLøpendeOvergangsstønad(it.behandlingId, it.personIdent)
                        val avsluttOppgaveMedOppdatertBeskrivelse = it.oppgave.copy(beskrivelse = it.oppgave.beskrivelse + "\nAutomatisk avsluttet oppgave: Ingen endring i inntekt.", status = StatusEnum.FERDIGSTILT)
                        oppgaveService.oppdaterOppgave(avsluttOppgaveMedOppdatertBeskrivelse)
                    }
                } else {
                    beOmRegnskap(it.personIdent, it.behandlingId)
                }
            }
            opprettFremleggHvisOvergangsstønadMerEnn4MndIÅr(it.behandlingId, it.oppgave, it.tilkjentYtelse)
        }

        return fagsakIds
    }

    private fun hentNæringsinntektDataForBeregning(oppgaver: List<Oppgave>): List<NæringsinntektDataForBeregning> {
        val næringsinntektDataForBeregningList =
            oppgaver.map {
                val personIdent =
                    it.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                        ?: it.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${it.id}")
                val fagsakOvergangsstønad = fagsakService.finnFagsaker(setOf(personIdentForOppgave(it))).firstOrNull { it.stønadstype == StønadType.OVERGANGSSTØNAD } ?: throw RuntimeException("Fant ikke fagsak for overgangsstønad for person: ${personIdentForOppgave(it)}")
                val behandlingId = behandlingService.finnSisteIverksatteBehandling(fagsakOvergangsstønad.id)?.id ?: throw RuntimeException("Fant ingen gjeldende behandling for fagsakId: ${fagsakOvergangsstønad.id}")
                val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
                val næringsinntektDataForBeregning =
                    NæringsinntektDataForBeregning(
                        oppgave = it,
                        personIdent = personIdent,
                        fagsak = fagsakOvergangsstønad,
                        behandlingId = behandlingId,
                        tilkjentYtelse = tilkjentYtelse,
                        fjoråretsNæringsinntekt = hentFjoråretsNæringsinntekt(fagsakOvergangsstønad.fagsakPersonId),
                        fjoråretsPersonInntekt = inntektService.hentÅrsinntekt(personIdent, årstallIFjor),
                        forventetInntektIFjor = forventetInntektSnittIFjor(tilkjentYtelse),
                    )
                secureLogger.info("Næringsinntektsdata for beregning: $næringsinntektDataForBeregning")
                næringsinntektDataForBeregning
            }

        return næringsinntektDataForBeregningList
    }

    private fun personIdentForOppgave(it: Oppgave) =
        (
            it.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                ?: it.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${it.id}")
        )

    private fun giBeskjedOmKontrollertInntektVedLøpendeOvergangsstønad(
        behandlingId: UUID,
        personIdent: String,
    ) {
        if (tilkjentYtelseService.harLøpendeUtbetaling(behandlingId)) {
            næringsinntektBrukernotifikasjonService.sendBeskjedTilBruker(personIdent, behandlingId, "Inntekt sjekket for $årstallIFjor. Meld inn dersom det er endringer i inntekt.")
        }
    }

    private fun giVarselOmNyVurderingAvInntekt(
        behandlingId: UUID,
        personIdent: String,
    ) {
        næringsinntektBrukernotifikasjonService.sendBeskjedTilBruker(personIdent, behandlingId, "Inntekt sjekket for $årstallIFjor. Det er oppdaget 10% endring i inntekt eller mer enn forventet inntekt.")
    }

    private fun opprettFremleggHvisOvergangsstønadMerEnn4MndIÅr(
        behandlingId: UUID,
        oppgave: Oppgave,
        tilkjentYtelse: TilkjentYtelse,
    ) {
        val antallMåneder = antallMånederMedVedtakForÅr(tilkjentYtelse, YearMonth.now().year)
        if (antallMåneder > 3) {
            oppgaveService.opprettOppgave(behandlingId = behandlingId, oppgavetype = Oppgavetype.Fremlegg, fristFerdigstillelse = LocalDate.of(YearMonth.now().plusYears(1).year, 12, 15), mappeId = oppgave.mappeId)
        }
    }

    private fun oppfyllerAktivitetsplikt(
        fjoråretsPersonInntekt: Int,
        fjoråretsNæringsinntekt: Int,
        fagsakOvergangsstønad: Fagsak,
    ): Boolean {
        secureLogger.info("Forrige års inntekt for person uten ytelse fra offentlig: $fjoråretsPersonInntekt - næringsinntekt: $fjoråretsNæringsinntekt (Fagsak: ${fagsakOvergangsstønad.id})")
        return fjoråretsNæringsinntekt > INNTEKTSGRENSE_NÆRING || (fjoråretsPersonInntekt + fjoråretsNæringsinntekt) > INNTEKTSGRENSE_PERSON_OG_NÆRING
    }

    private fun beOmRegnskap(
        personIdent: String,
        behandlingId: UUID,
    ) {
        næringsinntektBrukernotifikasjonService.sendBeskjedTilBruker(personIdent, behandlingId, "I forbindelse med inntektskontroll for selvstendig næringsdrivende ber vi om at du sender inn regnskap.")
    }

    private fun skalKontrolleres(
        tilkjentYtelse: TilkjentYtelse,
        fagsakOvergangsstønad: Fagsak,
    ): Boolean {
        val antallMåneder = antallMånederMedVedtakForÅr(tilkjentYtelse, årstallIFjor)
        secureLogger.info("$antallMåneder måneder med vedtak for fagsakId: ${fagsakOvergangsstønad.id} eksternFagsakId: ${fagsakOvergangsstønad.eksternId}")
        return antallMåneder > 3
    }

    private fun hentFjoråretsNæringsinntekt(fagsakPersonId: UUID): Int {
        val inntekt = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId).filter { it.inntektsår == årstallIFjor }
        val næringsinntekt = inntekt.sumOf { it.næring } + inntekt.sumOf { it.svalbard?.næring ?: 0 }
        secureLogger.info("Inntekt for person $inntekt - næringsinntekt er beregnet til: $næringsinntekt")
        return næringsinntekt
    }

    private fun forventetInntektSnittIFjor(tilkjentYtelse: TilkjentYtelse): Int {
        val antallMånederInntektList =
            tilkjentYtelse.andelerTilkjentYtelse.map {
                (
                    it.periode
                        .snitt(Månedsperiode(YearMonth.of(årstallIFjor, 1), YearMonth.of(årstallIFjor, 12)))
                        ?.lengdeIHeleMåneder()
                        ?.toInt() ?: 0
                ) to it.inntekt
            }

        val sumAntallMånederStønadIFjor = antallMånederInntektList.sumOf { it.first }
        if (sumAntallMånederStønadIFjor == 0) return 0

        return antallMånederInntektList.sumOf { (inntekt, antallMåneder) ->
            inntekt * (antallMåneder / sumAntallMånederStønadIFjor)
        }
    }

    private fun hentOppgaverForSelvstendigeTilInntektskontroll(): List<Oppgave> {
        val mappeIdForSelvstendigNæringsdrivende = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { it.navn == "61 Selvstendig næringsdrivende" }.id
        secureLogger.info("MappeId for selvstendige: $mappeIdForSelvstendigNæringsdrivende")
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.ENF,
                behandlingstema = Behandlingstema.Overgangsstønad,
                oppgavetype = Oppgavetype.Fremlegg,
                fristTomDato = LocalDate.of(YearMonth.now().year, 12, 15),
                mappeId = mappeIdForSelvstendigNæringsdrivende.toLong(),
            )
        val oppgaverForSelvstendige = oppgaveService.hentOppgaver(finnOppgaveRequest)
        secureLogger.info("Antall oppgaver for selvstendige med frist 15. desember: ${oppgaverForSelvstendige.oppgaver.size}")

        if (oppgaverForSelvstendige.oppgaver.isEmpty() ||
            oppgaverForSelvstendige.oppgaver
                .first()
                .identer
                ?.any { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT } == false
        ) {
            secureLogger.info("Fant ingen oppgaver for selvstendige med frist 15. desember")
            return emptyList()
        }
        return oppgaverForSelvstendige.oppgaver
    }

    private fun antallMånederMedVedtakForÅr(
        tilkjentYtelse: TilkjentYtelse,
        årstall: Int,
    ): Long {
        val perioder = tilkjentYtelse.andelerTilkjentYtelse.map { it.periode.snitt(Månedsperiode(YearMonth.of(årstall, 1), YearMonth.of(årstall, 12))) }
        val sum = perioder.sumOf { it?.lengdeIHeleMåneder() ?: 0 }
        return sum
    }

    companion object {
        private const val INNTEKTSGRENSE_NÆRING = 50_000
        private const val INNTEKTSGRENSE_PERSON_OG_NÆRING = 190_000
    }
}

data class NæringsinntektDataForBeregning(
    val oppgave: Oppgave,
    val personIdent: String,
    val fagsak: Fagsak,
    val behandlingId: UUID,
    val tilkjentYtelse: TilkjentYtelse,
    val fjoråretsNæringsinntekt: Int,
    val fjoråretsPersonInntekt: Int,
    val forventetInntektIFjor: Int,
)
