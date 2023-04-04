package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.getValue
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
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
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val fagsakService: FagsakService,
    private val oppgaveRepository: OppgaveRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val cacheManager: CacheManager,
    @Value("\${FRONTEND_OPPGAVE_URL}") private val frontendOppgaveUrl: URI,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettOppgave(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        tilordnetNavIdent: String? = null,
        beskrivelse: String? = null,
        mappeId: Long? = null, // Dersom denne er satt vil vi ikke prøve å finne mappe basert på oppgavens innhold
    ): Long {
        val oppgaveFinnesFraFør =
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)

        return if (oppgaveFinnesFraFør !== null) {
            oppgaveFinnesFraFør.gsakOppgaveId
        } else {
            val opprettetOppgaveId =
                opprettOppgaveUtenÅLagreIRepository(
                    behandlingId = behandlingId,
                    oppgavetype = oppgavetype,
                    fristFerdigstillelse = null,
                    beskrivelse = lagOppgaveTekst(beskrivelse),
                    tilordnetNavIdent = tilordnetNavIdent,
                    mappeId = mappeId,
                )
            val oppgave = EfOppgave(
                gsakOppgaveId = opprettetOppgaveId,
                behandlingId = behandlingId,
                type = oppgavetype,
            )
            oppgaveRepository.insert(oppgave)
            opprettetOppgaveId
        }
    }

    fun oppdaterOppgave(oppgave: Oppgave) {
        oppgaveClient.oppdaterOppgave(oppgave)
    }

    /**
     * I de tilfeller en service ønsker å ansvare selv for lagring til [OppgaveRepository]
     */
    fun opprettOppgaveUtenÅLagreIRepository(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        fristFerdigstillelse: LocalDate?,
        beskrivelse: String,
        tilordnetNavIdent: String?,
        mappeId: Long? = null, // Dersom denne er satt vil vi ikke prøve å finne mappe basert på oppgavens innhold
    ): Long {
        val settBehandlesAvApplikasjon = when (oppgavetype) {
            Oppgavetype.BehandleSak,
            Oppgavetype.BehandleUnderkjentVedtak,
            Oppgavetype.GodkjenneVedtak,
            -> true
            Oppgavetype.InnhentDokumentasjon -> false
            else -> error("Håndterer ikke behandlesAvApplikasjon for $oppgavetype")
        }
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val personIdent = fagsak.hentAktivIdent()
        val enhetsnummer = arbeidsfordelingService.hentNavEnhet(personIdent)?.enhetId
        val opprettOppgave = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
            saksId = fagsak.eksternId.id.toString(),
            tema = Tema.ENF,
            oppgavetype = oppgavetype,
            fristFerdigstillelse = fristFerdigstillelse ?: lagFristForOppgave(LocalDateTime.now()),
            beskrivelse = beskrivelse,
            enhetsnummer = enhetsnummer,
            behandlingstema = finnBehandlingstema(fagsak.stønadstype).value,
            tilordnetRessurs = tilordnetNavIdent,
            behandlesAvApplikasjon = if (settBehandlesAvApplikasjon) "familie-ef-sak" else null,
            mappeId = mappeId ?: finnAktuellMappe(enhetsnummer, oppgavetype),
        )

        return try {
            oppgaveClient.opprettOppgave(opprettOppgave)
        } catch (e: Exception) {
            if (finnerIkkeGyldigArbeidsfordeling(e)) {
                oppgaveClient.opprettOppgave(opprettOppgave.copy(enhetsnummer = ENHET_NR_NAY))
            } else {
                throw e
            }
        }
    }

    private fun finnerIkkeGyldigArbeidsfordeling(e: Exception): Boolean =
        e.message?.contains("Fant ingen gyldig arbeidsfordeling for oppgaven") ?: false

    private fun finnAktuellMappe(enhetsnummer: String?, oppgavetype: Oppgavetype): Long? {
        if (enhetsnummer == "4489" && oppgavetype == Oppgavetype.GodkjenneVedtak) {
            val mapper = finnMapper(enhetsnummer)
            val mappeIdForGodkjenneVedtak = mapper.find {
                (it.navn.contains("70 Godkjennevedtak") || it.navn.contains("70 Godkjenne vedtak")) &&
                    !it.navn.contains("EF Sak")
            }?.id?.toLong()
            mappeIdForGodkjenneVedtak?.let {
                logger.info("Legger oppgave i Godkjenne vedtak-mappe")
            } ?: run {
                logger.error("Fant ikke mappe for godkjenne vedtak: 70 Godkjenne vedtak for enhetsnummer=$enhetsnummer")
            }
            return mappeIdForGodkjenneVedtak
        }
        if (enhetsnummer == "4489" && oppgavetype == Oppgavetype.InnhentDokumentasjon) { // Skjermede personer skal ikke puttes i mappe
            return finnHendelseMappeId(enhetsnummer)
        }
        return null
    }

    fun finnHendelseMappeId(enhetsnummer: String): Long? {
        val mapperResponse = oppgaveClient.finnMapper(enhetsnummer, 1000)
        val mappe = mapperResponse.mapper.find {
            it.navn.contains("62 Hendelser") && !it.navn.contains("EF Sak")
        }
            ?: error("Fant ikke mappe for hendelser")
        return mappe.id.toLong()
    }

    fun fordelOppgave(gsakOppgaveId: Long, saksbehandler: String, versjon: Int? = null): Long {
        return oppgaveClient.fordelOppgave(
            gsakOppgaveId,
            saksbehandler,
            versjon,
        )
    }

    fun tilbakestillFordelingPåOppgave(gsakOppgaveId: Long, versjon: Int? = null): Long {
        return oppgaveClient.fordelOppgave(gsakOppgaveId, null, versjon = versjon)
    }

    fun hentOppgaveSomIkkeErFerdigstilt(oppgavetype: Oppgavetype, saksbehandling: Saksbehandling): EfOppgave? {
        return oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(saksbehandling.id, oppgavetype)
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

    /**
     * @param ignorerFeilregistrert ignorerer oppgaver som allerede er feilregistrerte
     * Den burde kun settes til true for lukking av oppgaver koblet til henleggelse
     * Oppgaver skal ikke være lukket når denne kalles, då det er ef-sak som burde lukke oppgaver som vi har opprettet
     */
    fun ferdigstillOppgaveHvisOppgaveFinnes(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        ignorerFeilregistrert: Boolean = false,
    ) {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
        oppgave?.let {
            ferdigstillOppgaveOgSettEfOppgaveTilFerdig(oppgave, ignorerFeilregistrert)
        }
    }

    private fun ferdigstillOppgaveOgSettEfOppgaveTilFerdig(oppgave: EfOppgave, ignorerFeilregistrert: Boolean = false) {
        try {
            ferdigstillOppgave(oppgave.gsakOppgaveId)
        } catch (e: RessursException) {
            if (ignorerFeilregistrert && e.ressurs.melding.contains("Oppgave har status feilregistrert")) {
                logger.warn("Ignorerer ferdigstill av oppgave=${oppgave.gsakOppgaveId} som har status feilregistrert")
            } else {
                throw e
            }
        }
        oppgave.erFerdigstilt = true
        oppgaveRepository.update(oppgave)
    }

    fun ferdigstillOppgave(gsakOppgaveId: Long) {
        oppgaveClient.ferdigstillOppgave(gsakOppgaveId)
    }

    fun finnSisteOppgaveForBehandling(behandlingId: UUID): EfOppgave? {
        return oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId)
    }

    fun hentIkkeFerdigstiltOppgaveForBehandling(behandlingId: UUID): Oppgave? {
        return oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandlingId, setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak))
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }
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

    private fun finnBehandlingstema(stønadstype: StønadType): Behandlingstema {
        return when (stønadstype) {
            StønadType.OVERGANGSSTØNAD -> Behandlingstema.Overgangsstønad
            StønadType.BARNETILSYN -> Behandlingstema.Barnetilsyn
            StønadType.SKOLEPENGER -> Behandlingstema.Skolepenger
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

    fun finnMapper(enheter: List<String>): List<MappeDto> {
        return enheter.flatMap { finnMapper(it) }
    }

    fun finnMapper(enhet: String): List<MappeDto> {
        return cacheManager.getValue("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons = oppgaveClient.finnMapper(
                enhetsnummer = enhet,
                limit = 1000,
            )
            if (mappeRespons.antallTreffTotalt > mappeRespons.mapper.size) {
                logger.error(
                    "Det finnes flere mapper (${mappeRespons.antallTreffTotalt}) " +
                        "enn vi har hentet ut (${mappeRespons.mapper.size}). Sjekk limit. ",
                )
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
