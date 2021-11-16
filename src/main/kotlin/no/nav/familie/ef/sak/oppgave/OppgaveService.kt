package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.infrastruktur.config.getOrThrow
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.net.URI
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EfOppgave


@Service
class OppgaveService(private val oppgaveClient: OppgaveClient,
                     private val fagsakRepository: FagsakRepository,
                     private val oppgaveRepository: OppgaveRepository,
                     private val arbeidsfordelingService: ArbeidsfordelingService,
                     private val pdlClient: PdlClient,
                     private val cacheManager: CacheManager,
                     @Value("\${FRONTEND_OPPGAVE_URL}") private val frontendOppgaveUrl: URI) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettOppgave(behandlingId: UUID,
                       oppgavetype: Oppgavetype,
                       tilordnetNavIdent: String? = null,
                       beskrivelse: String? = null): Long {
        val fagsak = fagsakRepository.finnFagsakTilBehandling(behandlingId)
                     ?: error("Finner ikke fagsak til behandlingDd=${behandlingId}")

        val oppgaveFinnesFraFør = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

        return if (oppgaveFinnesFraFør !== null) {
            oppgaveFinnesFraFør.gsakOppgaveId
        } else {

            val aktørId = pdlClient.hentAktørIder(fagsak.hentAktivIdent()).identer.first().ident
            val enhetsnummer = arbeidsfordelingService.hentNavEnhet(fagsak.hentAktivIdent())?.enhetId
            val opprettOppgave =
                    OpprettOppgaveRequest(ident = OppgaveIdentV2(ident = aktørId, gruppe = IdentGruppe.AKTOERID),
                                          saksId = fagsak.eksternId.id.toString(),
                                          tema = Tema.ENF,
                                          oppgavetype = oppgavetype,
                                          fristFerdigstillelse = lagFristForOppgave(LocalDateTime.now()),
                                          beskrivelse = lagOppgaveTekst(beskrivelse),
                                          enhetsnummer = enhetsnummer,
                                          behandlingstema = finnBehandlingstema(fagsak.stønadstype).value,
                                          tilordnetRessurs = tilordnetNavIdent,
                                          behandlesAvApplikasjon = "familie-ef-sak",
                                          mappeId = finnAktuellMappe(enhetsnummer, oppgavetype)
                    )

            val opprettetOppgaveId = oppgaveClient.opprettOppgave(opprettOppgave)

            val oppgave = EfOppgave(gsakOppgaveId = opprettetOppgaveId,
                                    behandlingId = behandlingId,
                                    type = oppgavetype)
            oppgaveRepository.insert(oppgave)
            opprettetOppgaveId
        }
    }

    private fun finnAktuellMappe(enhetsnummer: String?, oppgavetype: Oppgavetype): Long? {
        if (enhetsnummer == "4489" && oppgavetype == Oppgavetype.GodkjenneVedtak) {
            val mapper = finnMapper("4489")
            val mappeIdForGodkjenneVedtak = mapper.find { it.navn.contains("EF Sak - 70 Godkjenne vedtak") }?.id?.toLong()
            mappeIdForGodkjenneVedtak?.let {
                logger.info("Legger oppgave i Godkjenne vedtak-mappe")
            } ?: run {
                logger.error("Fant ikke mappe for godkjenne vedtak: EF Sak - 70 Godkjenne vedtak")
            }
            return mappeIdForGodkjenneVedtak
        }
        return null
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

    fun hentEfOppgave(gsakOppgaveId: Long): EfOppgave? {
        return oppgaveRepository.findByGsakOppgaveId(gsakOppgaveId)
    }

    fun ferdigstillBehandleOppgave(behandlingId: UUID, oppgavetype: Oppgavetype) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
                      ?: error("Finner ikke oppgave for behandling $behandlingId")
        ferdigstillOppgaveOgSettEfOppgaveTilFerdig(oppgave)
    }

    fun ferdigstillOppgaveHvisOppgaveFinnes(behandlingId: UUID, oppgavetype: Oppgavetype) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
        oppgave?.let {
            ferdigstillOppgaveOgSettEfOppgaveTilFerdig(oppgave)
        }
    }

    private fun ferdigstillOppgaveOgSettEfOppgaveTilFerdig(oppgave: EfOppgave) {
        ferdigstillOppgave(oppgave.gsakOppgaveId)
        oppgave.erFerdigstilt = true
        oppgaveRepository.update(oppgave)
    }

    fun ferdigstillOppgave(gsakOppgaveId: Long) {
        oppgaveClient.ferdigstillOppgave(gsakOppgaveId)
    }

    fun finnSisteOppgaveForBehandling(behandlingId: UUID): EfOppgave {
        return oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId)
    }

    fun lagOppgaveTekst(beskrivelse: String? = null): String {
        return if (beskrivelse != null) {
            beskrivelse + "\n"
        } else {
            ""
        } +
               "----- Opprettet av familie-ef-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
               "$frontendOppgaveUrl" + "\n----- Oppgave må behandles i ny løsning"
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

    /**
     * Frist skal være 1 dag hvis den opprettes før kl. 12
     * og 2 dager hvis den opprettes etter kl. 12
     *
     * Helgedager må ekskluderes
     *
     */
    fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val frist = when (gjeldendeTid.dayOfWeek) {
            DayOfWeek.FRIDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2))
            DayOfWeek.SATURDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(2).withHour(8))
            DayOfWeek.SUNDAY -> fristBasertPåKlokkeslett(gjeldendeTid.plusDays(1).withHour(8))
            else -> fristBasertPåKlokkeslett(gjeldendeTid)
        }

        return when (frist.dayOfWeek) {
            DayOfWeek.SATURDAY -> frist.plusDays(2)
            DayOfWeek.SUNDAY -> frist.plusDays(1)
            else -> frist
        }
    }

    fun finnMapper(enhet: String): List<MappeDto> {
        return cacheManager.getOrThrow("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons = oppgaveClient.finnMapper(FinnMappeRequest(tema = listOf(),
                                                                         enhetsnr = enhet,
                                                                         opprettetFom = null,
                                                                         limit = 1000))
            if (mappeRespons.antallTreffTotalt > mappeRespons.mapper.size) {
                logger.error("Det finnes flere mapper (${mappeRespons.antallTreffTotalt}) " +
                             "enn vi har hentet ut (${mappeRespons.mapper.size}). Sjekk limit. ")
            }
            mappeRespons.mapper
        }
    }

    private fun fristBasertPåKlokkeslett(gjeldendeTid: LocalDateTime): LocalDate {
        return if (gjeldendeTid.hour >= 12) {
            return gjeldendeTid.plusDays(2).toLocalDate()
        } else {
            gjeldendeTid.plusDays(1).toLocalDate()
        }
    }

}
