package no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
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
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class NæringsinntektKontrollService(
    val næringsinntektKontrollRepository: NæringsinntektKontrollRepository,
    val oppgaveService: OppgaveService,
    val tilkjentYtelseService: TilkjentYtelseService,
    val næringsinntektDataForBeregningService: NæringsinntektDataForBeregningService,
    val næringsinntektBrukernotifikasjonService: NæringsinntektBrukernotifikasjonService,
    val næringsinntektNotatService: NæringsinntektNotatService,
    val næringsinntektKontrollBrev: NæringsinntektKontrollBrev,
    val taskService: TaskService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun opprettTasksForSelvstendigeTilInntektskontroll() {
        val oppgaver = hentOppgaverForSelvstendigeTilInntektskontroll()
        oppgaver.forEach {
            val næringsinntektKontrollTask = NæringsinntektKontrollForOppgaveTask.opprettTask(it.id ?: throw Feil("Feil i inntektskontroll for næringsdrivende: Oppgave må ha id for at den kan behandles"))
            secureLogger.info("Lagrer ned næringsinntektKontrollTask: $næringsinntektKontrollTask")
            val saved = taskService.save(næringsinntektKontrollTask)
            secureLogger.info("Lagret task: $saved")
        }
    }

    fun kontrollerInntektForSelvstendigNæringsdrivende(
        årstallIFjor: Int,
        oppgaveId: Long,
    ) {
        if (LeaderClient.isLeader() == false) return
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val næringsinntektDataForBeregning = næringsinntektDataForBeregningService.hentNæringsinntektDataForBeregning(oppgave, årstallIFjor)

        if (næringsinntektDataForBeregning.skalKontrolleres()) {
            if (næringsinntektDataForBeregning.oppfyllerAktivitetsplikt()) {
                if (næringsinntektDataForBeregning.har10ProsentØkningEllerMer()) {
                    secureLogger.info("Har 10% høyere næringsinntekt for person: ${næringsinntektDataForBeregning.personIdent} (Næringsinntekt: ${næringsinntektDataForBeregning.forventetInntektIFjor} - ForventetInntekt: ${næringsinntektDataForBeregning.forventetInntektIFjor})")
                    giVarselOmNyVurderingAvInntekt(næringsinntektDataForBeregning.behandlingId, næringsinntektDataForBeregning.personIdent, årstallIFjor)
                    val oppgaveMedUtsattFrist = næringsinntektDataForBeregning.oppgave.copy(fristFerdigstillelse = LocalDate.of(årstallIFjor + 2, 1, 11).toString())
                    oppgaveService.oppdaterOppgave(oppgaveMedUtsattFrist)
                    næringsinntektKontrollBrev.sendBrev(næringsinntektDataForBeregning) // Vurderes om skal tas i bruk eller ikke høst 2025. Favro: NAV-24146
                    næringsinntektKontrollRepository.insert(NæringsinntektKontrollDomain(oppgaveId = oppgaveId, fagsakId = næringsinntektDataForBeregning.fagsak.id, utfall = NæringsinntektKontrollUtfall.MINIMUM_TI_PROSENT_ENDRING_I_INNTEKT))
                } else {
                    giBeskjedOmKontrollertInntektVedLøpendeOvergangsstønad(næringsinntektDataForBeregning.behandlingId, næringsinntektDataForBeregning.personIdent, årstallIFjor)
                    val avsluttOppgaveMedOppdatertBeskrivelse = næringsinntektDataForBeregning.oppgave.copy(beskrivelse = næringsinntektDataForBeregning.oppgave.beskrivelse + "\nAutomatisk avsluttet oppgave: Ingen endring i inntekt.", status = StatusEnum.FERDIGSTILT)
                    oppgaveService.oppdaterOppgave(avsluttOppgaveMedOppdatertBeskrivelse)
                    næringsinntektNotatService.lagNotat(næringsinntektDataForBeregning) // Må arkiveres / journalføres
                    // Journalfør notat
                    næringsinntektKontrollRepository.insert(NæringsinntektKontrollDomain(oppgaveId = oppgaveId, fagsakId = næringsinntektDataForBeregning.fagsak.id, utfall = NæringsinntektKontrollUtfall.UENDRET_INNTEKT))
                }
            } else {
                beOmRegnskap(næringsinntektDataForBeregning.personIdent, næringsinntektDataForBeregning.behandlingId)
                næringsinntektKontrollRepository.insert(NæringsinntektKontrollDomain(oppgaveId = oppgaveId, fagsakId = næringsinntektDataForBeregning.fagsak.id, utfall = NæringsinntektKontrollUtfall.OPPFYLLER_IKKE_AKTIVITETSPLIKT))
            }
        } else {
            næringsinntektKontrollRepository.insert(NæringsinntektKontrollDomain(oppgaveId = oppgaveId, fagsakId = næringsinntektDataForBeregning.fagsak.id, utfall = NæringsinntektKontrollUtfall.KONTROLLERES_IKKE))
        }
        opprettFremleggHvisOvergangsstønadMerEnn4MndIÅr(næringsinntektDataForBeregning)
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
