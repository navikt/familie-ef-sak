package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.OppgaveClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.oppgave.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.familie.ef.sak.repository.domain.Oppgave as EfOppgave


@Service
class OppgaveService(private val oppgaveClient: OppgaveClient,
                     private val behandlingRepository: BehandlingRepository,
                     private val fagsakRepository: FagsakRepository,
                     private val oppgaveRepository: OppgaveRepository,
                     private val arbeidsfordelingService: ArbeidsfordelingService,
                     @Value("\${FRONTEND_OPPGAVE_URL}") private val frontendOppgaveUrl: URI) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettOppgave(behandlingId: UUID,
                       oppgavetype: Oppgavetype,
                       fristForFerdigstillelse: LocalDate,
                       enhetId: String? = null,
                       tilordnetNavIdent: String? = null,
                       beskrivelse: String? = null): Long {
        val behandling =
                behandlingRepository.findByIdOrNull(behandlingId) ?: error("Finner ikke behandling med id=${behandlingId}")
        val fagsak =
                fagsakRepository.findByIdOrNull(behandling.fagsakId) ?: error("Finner ikke fagsak med id=${behandling.fagsakId}")

        val eksisterendeOppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

        return if (eksisterendeOppgave != null && oppgavetype == eksisterendeOppgave.type) {
            logger.error("Fant eksisterende oppgave med samme oppgavetype som ikke er ferdigstilt ved opprettelse av ny oppgave ${eksisterendeOppgave}. " +
                         "Vi går videre, men kaster feil for å følge med på utviklingen.")

            eksisterendeOppgave.gsakOppgaveId
        } else {
            val enhetsnummer = arbeidsfordelingService.hentNavEnhet(fagsak.hentAktivIdent())
            val opprettOppgave = OpprettOppgaveRequest(
                    ident = OppgaveIdentV2(ident = fagsak.hentAktivIdent(), gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                    saksId = fagsak.id.toString(),
                    tema = Tema.ENF,
                    oppgavetype = oppgavetype,
                    fristFerdigstillelse = fristForFerdigstillelse,
                    beskrivelse = lagOppgaveTekst(fagsak.id.toString(), beskrivelse),
                    enhetsnummer = enhetId ?: enhetsnummer?.enhetId,
                    behandlingstema = finnBehandlingstema(fagsak.stønadstype).value,
                    tilordnetRessurs = tilordnetNavIdent
            )

            val opprettetOppgaveId = oppgaveClient.opprettOppgave(opprettOppgave)

            val oppgave = EfOppgave(gsakOppgaveId = opprettetOppgaveId,
                                    behandlingId = behandling.id,
                                    type = oppgavetype)
            oppgaveRepository.update(oppgave)
            opprettetOppgaveId
        }
    }

    fun fordelOppgave(gsakOppgaveId: Long, saksbehandler: String): Long {
        return oppgaveClient.fordelOppgave(gsakOppgaveId, saksbehandler)
    }

    fun tilbakestillFordelingPåOppgave(gsakOppgaveId: Long): Long {
        return oppgaveClient.fordelOppgave(gsakOppgaveId, null)
    }

    fun hentOppgaveSomIkkeErFerdigstilt(oppgavetype: Oppgavetype, behandling: Behandling): EfOppgave? {
        return oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, oppgavetype)
    }

    fun hentOppgave(gsakOppgaveId: Long): Oppgave {
        return oppgaveClient.finnOppgaveMedId(gsakOppgaveId)
    }

    fun ferdigstillBehandleOppgave(behandlingId: UUID, oppgavetype: Oppgavetype) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
                      ?: error("Finner ikke oppgave for behandling $behandlingId")
        ferdigstillOppgave(oppgave.gsakOppgaveId)

        oppgave.erFerdigstilt = true
        oppgaveRepository.update(oppgave)
    }

    fun ferdigstillOppgave(gsakOppgaveId: Long){
        oppgaveClient.ferdigstillOppgave(gsakOppgaveId)
    }

    fun lagOppgaveTekst(fagsakId: String, beskrivelse: String? = null): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } +
               "----- Opprettet av familie-ef-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
               "${frontendOppgaveUrl}/${fagsakId}"
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        return oppgaveClient.hentOppgaver(finnOppgaveRequest)
    }

    private fun finnBehandlingstema(stønadstype: Stønadstype): Behandlingstema {
        return when (stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> Behandlingstema.Overgangsstønad
            Stønadstype.BARNETILSYN -> Behandlingstema.Barnetilsyn
            Stønadstype.SKOLEPENGER -> Behandlingstema.Skolepenger
        }
    }

}
