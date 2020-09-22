package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.OppgaveClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.oppgave.*
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.familie.ef.sak.repository.domain.Oppgave as EFOppgave


@Service
class OppgaveService(private val oppgaveClient: OppgaveClient,
                     private val behandlingRepository: BehandlingRepository,
                     private val fagsakRepository: FagsakRepository,
                     private val oppgaveRepository: OppgaveRepository,
                     private val arbeidsfordelingService: ArbeidsfordelingService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun opprettOppgave(behandlingId: UUID,
                       oppgavetype: Oppgavetype,
                       fristForFerdigstillelse: LocalDate,
                       enhetId: String? = null,
                       tilordnetNavIdent: String? = null,
                       beskrivelse: String? = null): String {
        val behandling =
                behandlingRepository.findByIdOrNull(behandlingId) ?: error("Finner ikke behandling med id=${behandlingId}")
        val fagsak =
                fagsakRepository.findByIdOrNull(behandling.fagsakId) ?: error("Finner ikke fagsak med id=${behandling.fagsakId}")

        val eksisterendeOppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

        return if (eksisterendeOppgave != null && oppgavetype == eksisterendeOppgave.type) {
            logger.error("Fant eksisterende oppgave med samme oppgavetype som ikke er ferdigstilt ved opprettelse av ny oppgave ${eksisterendeOppgave}. " +
                         "Vi går videre, men kaster feil for å følge med på utviklingen.")

            eksisterendeOppgave.gsakId
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
                    behandlingstema = finnBehandlingstema(fagsak.stønadstype).kode,
                    tilordnetRessurs = tilordnetNavIdent
            )

            val opprettetOppgaveId = oppgaveClient.opprettOppgave(opprettOppgave)

            val oppgave = EFOppgave(gsakId = opprettetOppgaveId,
                                    behandlingId = behandling.id,
                                    type = oppgavetype)
            oppgaveRepository.save(oppgave)
            opprettetOppgaveId
        }
    }

    //    fun fordelOppgave(oppgaveId: Long, saksbehandler: String): String {
//        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler)
//    }
//
//    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): String {
//        return integrasjonClient.fordelOppgave(oppgaveId, null)
//    }
//
//    fun hentOppgaveSomIkkeErFerdigstilt(oppgavetype: Oppgavetype, behandling: Behandling): Oppgave? {
//        return oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)
//    }
//
//    fun hentOppgave(oppgaveId: Long): Oppgave {
//        return integrasjonClient.finnOppgaveMedId(oppgaveId)
//    }
//
//    fun ferdigstillOppgave(behandlingId: Long, oppgavetype: Oppgavetype) {
//        val oppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype,
//                                                                                         behandlingRepository.finnBehandling(
//                                                                                                 behandlingId))
//                      ?: error("Finner ikke oppgave for behandling $behandlingId")
//        integrasjonClient.ferdigstillOppgave(oppgave.gsakId.toLong())
//
//        oppgave.erFerdigstilt = true
//        oppgaveRepository.save(oppgave)
//    }
//
    fun lagOppgaveTekst(fagsakId: String, beskrivelse: String? = null): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } +
               "----- Opprettet av familie-ef-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
               "https://ensligmorellerfar.prod-fss.nais.io/fagsak/${fagsakId}" // TODO: Denne bør konfigureres slik at url er riktig
    }

    //
//    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): OppgaverOgAntall {
//        return integrasjonClient.hentOppgaver(finnOppgaveRequest)
//    }
//
    enum class Behandlingstema(val kode: String) {

        SKOLEPENGER("ab0177"),
        BARNETILSYN("ab0028"),
        OVERGANGSSTØNAD("ab0071")
    }

    private fun finnBehandlingstema(stønadstype: Stønadstype): Behandlingstema {
        return when (stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> Behandlingstema.OVERGANGSSTØNAD
            Stønadstype.BARNETILSYN -> Behandlingstema.BARNETILSYN
            Stønadstype.SKOLEPENGER -> Behandlingstema.SKOLEPENGER
        }
    }
//
//    companion object {
//        val LOG = LoggerFactory.getLogger(this::class.java)
//    }
}