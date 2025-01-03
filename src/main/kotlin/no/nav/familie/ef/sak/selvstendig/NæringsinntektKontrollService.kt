package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
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
    val tilkjentYtelseService: TilkjentYtelseService,
    val næringsinntektDataForBeregningService: NæringsinntektDataForBeregningService,
    val næringsinntektBrukernotifikasjonService: NæringsinntektBrukernotifikasjonService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun kontrollerInntektForSelvstendigNæringsdrivende(årstallIFjor: Int) {
        if (LeaderClient.isLeader() == false) return

        val oppgaver = hentOppgaverForSelvstendigeTilInntektskontroll()

        val næringsinntektDataForBeregningList = næringsinntektDataForBeregningService.hentNæringsinntektDataForBeregning(oppgaver, årstallIFjor)

        næringsinntektDataForBeregningList.forEach {
            if (it.skalKontrolleres()) {
                if (it.oppfyllerAktivitetsplikt()) {
                    if (it.har10ProsentØkningEllerMer()) {
                        secureLogger.info("Har 10% høyere næringsinntekt for person: ${it.personIdent} (Næringsinntekt: ${it.forventetInntektIFjor} - ForventetInntekt: ${it.forventetInntektIFjor})")
                        giVarselOmNyVurderingAvInntekt(it.behandlingId, it.personIdent, årstallIFjor)
                        val oppgaveMedUtsattFrist = it.oppgave.copy(fristFerdigstillelse = LocalDate.of(årstallIFjor + 2, 1, 11).toString())
                        oppgaveService.oppdaterOppgave(oppgaveMedUtsattFrist)
                    } else {
                        // Lag notat
                        giBeskjedOmKontrollertInntektVedLøpendeOvergangsstønad(it.behandlingId, it.personIdent, årstallIFjor)
                        val avsluttOppgaveMedOppdatertBeskrivelse = it.oppgave.copy(beskrivelse = it.oppgave.beskrivelse + "\nAutomatisk avsluttet oppgave: Ingen endring i inntekt.", status = StatusEnum.FERDIGSTILT)
                        oppgaveService.oppdaterOppgave(avsluttOppgaveMedOppdatertBeskrivelse)
                    }
                } else {
                    beOmRegnskap(it.personIdent, it.behandlingId)
                }
            }
            opprettFremleggHvisOvergangsstønadMerEnn4MndIÅr(it)
        }
    }

    private fun giBeskjedOmKontrollertInntektVedLøpendeOvergangsstønad(
        behandlingId: UUID,
        personIdent: String,
        årstallIFjor: Int,
    ) {
        if (tilkjentYtelseService.harLøpendeUtbetaling(behandlingId)) {
            næringsinntektBrukernotifikasjonService.sendBeskjedTilBruker(personIdent, behandlingId, "Inntekt sjekket for $årstallIFjor. Meld inn dersom det er endringer i inntekt.")
        }
    }

    private fun giVarselOmNyVurderingAvInntekt(
        behandlingId: UUID,
        personIdent: String,
        årstallIFjor: Int,
    ) {
        næringsinntektBrukernotifikasjonService.sendBeskjedTilBruker(personIdent, behandlingId, "Inntekt sjekket for $årstallIFjor. Det er oppdaget 10% endring i inntekt eller mer enn forventet inntekt.")
    }

    private fun opprettFremleggHvisOvergangsstønadMerEnn4MndIÅr(
        næringsinntektDataForBeregning: NæringsinntektDataForBeregning,
    ) {
        val antallMåneder = næringsinntektDataForBeregning.antallMånederMedVedtakForÅr(YearMonth.now().year)
        if (antallMåneder > 3) {
            oppgaveService.opprettOppgave(behandlingId = næringsinntektDataForBeregning.behandlingId, oppgavetype = Oppgavetype.Fremlegg, fristFerdigstillelse = LocalDate.of(YearMonth.now().plusYears(1).year, 12, 15), mappeId = næringsinntektDataForBeregning.oppgave.mappeId)
        }
    }

    private fun beOmRegnskap(
        personIdent: String,
        behandlingId: UUID,
    ) {
        næringsinntektBrukernotifikasjonService.sendBeskjedTilBruker(personIdent, behandlingId, "I forbindelse med inntektskontroll for selvstendig næringsdrivende ber vi om at du sender inn regnskap.")
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
}
