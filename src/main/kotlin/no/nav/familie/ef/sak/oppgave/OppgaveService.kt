package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.config.getValue
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.lagOpprettOppgavebeskrivelse
import no.nav.familie.ef.sak.oppgave.dto.UtdanningOppgaveDto
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
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleSak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleUnderkjentVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.GodkjenneVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.InnhentDokumentasjon
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.VurderHenvendelse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EfOppgave

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val fagsakService: FagsakService,
    private val oppgaveRepository: OppgaveRepository,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val cacheManager: CacheManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettOppgave(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
        vurderHenvendelseOppgaveSubtype: OppgaveSubtype? = null,
        tilordnetNavIdent: String? = null,
        beskrivelse: String? = null,
        mappeId: Long? = null, // Dersom denne er satt vil vi ikke prøve å finne mappe basert på oppgavens innhold
        prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
        fristFerdigstillelse: LocalDate? = null,
    ): Long {
        val oppgaveFinnesFraFør = getOppgaveFinnesFraFør(oppgavetype, vurderHenvendelseOppgaveSubtype, behandlingId)
        return if (oppgaveFinnesFraFør !== null) {
            oppgaveFinnesFraFør.gsakOppgaveId
        } else {
            val opprettetOppgaveId =
                opprettOppgaveUtenÅLagreIRepository(
                    behandlingId = behandlingId,
                    oppgavetype = oppgavetype,
                    fristFerdigstillelse = fristFerdigstillelse,
                    beskrivelse = lagOpprettOppgavebeskrivelse(beskrivelse),
                    tilordnetNavIdent = tilordnetNavIdent,
                    mappeId = mappeId,
                    prioritet = prioritet,
                )
            val oppgave =
                EfOppgave(
                    gsakOppgaveId = opprettetOppgaveId,
                    behandlingId = behandlingId,
                    type = oppgavetype,
                    oppgaveSubtype = vurderHenvendelseOppgaveSubtype,
                )
            oppgaveRepository.insert(oppgave)
            opprettetOppgaveId
        }
    }

    private fun getOppgaveFinnesFraFør(
        oppgavetype: Oppgavetype,
        vurderHenvendelseOppgaveSubtype: OppgaveSubtype?,
        behandlingId: UUID,
    ) = if (oppgavetype == VurderHenvendelse && vurderHenvendelseOppgaveSubtype != null) {
        oppgaveRepository.findByBehandlingIdAndTypeAndOppgaveSubtype(
            behandlingId,
            oppgavetype,
            vurderHenvendelseOppgaveSubtype,
        )
    } else {
        oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
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
        prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    ): Long {
        val settBehandlesAvApplikasjon = utledSettBehandlesAvApplikasjon(oppgavetype)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val personIdent = fagsak.hentAktivIdent()
        val enhetsnummer = arbeidsfordelingService.hentNavEnhetId(personIdent, oppgavetype)
        val opprettOppgave =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = personIdent, gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                saksId = fagsak.eksternId.toString(),
                tema = Tema.ENF,
                oppgavetype = oppgavetype,
                fristFerdigstillelse = fristFerdigstillelse ?: lagFristForOppgave(LocalDateTime.now()),
                beskrivelse = beskrivelse,
                enhetsnummer = enhetsnummer,
                behandlingstema = finnBehandlingstema(fagsak.stønadstype).value,
                tilordnetRessurs = tilordnetNavIdent,
                behandlesAvApplikasjon = if (settBehandlesAvApplikasjon) "familie-ef-sak" else null,
                mappeId = mappeId ?: finnAktuellMappe(enhetsnummer, oppgavetype),
                prioritet = prioritet,
            )

        return try {
            oppgaveClient.opprettOppgave(opprettOppgave)
        } catch (e: Exception) {
            if (finnerIkkeGyldigArbeidsfordeling(e)) {
                oppgaveClient.opprettOppgave(opprettOppgave.copy(enhetsnummer = ENHET_NR_NAY))
            } else if (navIdentHarIkkeTilgangTilEnheten(e)) {
                oppgaveClient.opprettOppgave(opprettOppgave.copy(tilordnetRessurs = null))
            } else {
                throw e
            }
        }
    }

    private fun finnerIkkeGyldigArbeidsfordeling(e: Exception): Boolean = e.message?.contains("Fant ingen gyldig arbeidsfordeling for oppgaven") ?: false

    private fun navIdentHarIkkeTilgangTilEnheten(e: Exception): Boolean = e.message?.contains("navIdent har ikke tilgang til enheten") ?: false

    private fun finnAktuellMappe(
        enhetsnummer: String?,
        oppgavetype: Oppgavetype,
    ): Long? {
        if (enhetsnummer == "4489" && oppgavetype == GodkjenneVedtak) {
            val mapper = finnMapper(enhetsnummer)
            val mappeIdForGodkjenneVedtak =
                mapper
                    .find {
                        (it.navn.contains("70 Godkjennevedtak") || it.navn.contains("70 Godkjenne vedtak")) &&
                            !it.navn.contains("EF Sak")
                    }?.id
                    ?.toLong()
            mappeIdForGodkjenneVedtak?.let {
                logger.info("Legger oppgave i Godkjenne vedtak-mappe")
            } ?: run {
                logger.error("Fant ikke mappe for godkjenne vedtak: 70 Godkjenne vedtak for enhetsnummer=$enhetsnummer")
            }
            return mappeIdForGodkjenneVedtak
        }
        if (enhetsnummer == "4489" && oppgavetype == InnhentDokumentasjon) { // Skjermede personer skal ikke puttes i mappe
            return finnHendelseMappeId(enhetsnummer)
        }
        return null
    }

    fun finnHendelseMappeId(enhetsnummer: String): Long? {
        val mapperResponse = oppgaveClient.finnMapper(enhetsnummer, 1000)
        val mappe =
            mapperResponse.mapper.find {
                it.navn.contains("62 Hendelser") && !it.navn.contains("EF Sak")
            }
                ?: error("Fant ikke mappe for hendelser")
        return mappe.id.toLong()
    }

    fun fordelOppgave(
        gsakOppgaveId: Long,
        saksbehandler: String,
        versjon: Int? = null,
    ): Long {
        val oppgave = hentOppgave(gsakOppgaveId)

        return if (oppgave.tilordnetRessurs == saksbehandler) {
            gsakOppgaveId
        } else {
            oppgaveClient.fordelOppgave(
                gsakOppgaveId,
                saksbehandler,
                versjon,
            )
        }
    }

    fun tilbakestillFordelingPåOppgave(
        gsakOppgaveId: Long,
        versjon: Int? = null,
    ): Long = oppgaveClient.fordelOppgave(gsakOppgaveId, null, versjon = versjon)

    fun hentOppgaveSomIkkeErFerdigstilt(
        oppgavetype: Oppgavetype,
        saksbehandling: Saksbehandling,
    ): EfOppgave? = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(saksbehandling.id, oppgavetype)

    fun hentOppgave(gsakOppgaveId: Long): Oppgave = oppgaveClient.finnOppgaveMedId(gsakOppgaveId)

    fun hentEfOppgave(gsakOppgaveId: Long): EfOppgave? = oppgaveRepository.findByGsakOppgaveId(gsakOppgaveId)

    fun ferdigstillBehandleOppgave(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ) {
        val oppgave =
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
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

    private fun ferdigstillOppgaveOgSettEfOppgaveTilFerdig(
        oppgave: EfOppgave,
        ignorerFeilregistrert: Boolean = false,
    ) {
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

    fun settEfOppgaveTilFerdig(
        behandlingId: UUID,
        oppgavetype: Oppgavetype,
    ): EfOppgave? {
        val oppgave = oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandlingId, oppgavetype)
        return oppgave?.let {
            oppgaveRepository.update(it.copy(erFerdigstilt = true))
        }
    }

    fun finnSisteBehandleSakOppgaveForBehandling(behandlingId: UUID): EfOppgave? =
        oppgaveRepository.findTopByBehandlingIdAndTypeOrderBySporbarOpprettetTidDesc(
            behandlingId,
            BehandleSak,
        )

    fun finnSisteOppgaveForBehandling(behandlingId: UUID): EfOppgave? = oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandlingId)

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto = oppgaveClient.hentOppgaver(finnOppgaveRequest)

    private fun finnBehandlingstema(stønadstype: StønadType): Behandlingstema =
        when (stønadstype) {
            StønadType.OVERGANGSSTØNAD -> Behandlingstema.Overgangsstønad
            StønadType.BARNETILSYN -> Behandlingstema.Barnetilsyn
            StønadType.SKOLEPENGER -> Behandlingstema.Skolepenger
        }

    /**
     * Frist skal være 1 dag hvis den opprettes før kl. 12
     * og 2 dager hvis den opprettes etter kl. 12
     *
     * Helgedager må ekskluderes
     *
     */
    fun lagFristForOppgave(gjeldendeTid: LocalDateTime): LocalDate {
        val frist =
            when (gjeldendeTid.dayOfWeek) {
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

    fun finnMapper(enheter: List<String>): List<MappeDto> = enheter.flatMap { finnMapper(it) }

    fun finnMapper(enhet: String): List<MappeDto> =
        cacheManager.getValue("oppgave-mappe", enhet) {
            logger.info("Henter mapper på nytt")
            val mappeRespons =
                oppgaveClient.finnMapper(
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

    private fun fristBasertPåKlokkeslett(gjeldendeTid: LocalDateTime): LocalDate {
        return if (gjeldendeTid.hour >= 12) {
            return gjeldendeTid.plusDays(2).toLocalDate()
        } else {
            gjeldendeTid.plusDays(1).toLocalDate()
        }
    }

    fun finnOppgaverIUtdanningsmappe(fristDato: LocalDate): List<UtdanningOppgaveDto> {
        val oppgaver =
            oppgaveClient
                .hentOppgaver(
                    FinnOppgaveRequest(
                        tema = Tema.ENF,
                        mappeId = 100026882, // Mappenavn: 64 - Utdanning
                        fristFomDato = fristDato,
                        fristTomDato = fristDato,
                    ),
                ).oppgaver

        return oppgaver.map { oppgave ->
            UtdanningOppgaveDto(
                oppgave.identer?.first { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident,
                oppgave.behandlingstema?.let { Behandlingstema.fromValue(it) },
                oppgave.oppgavetype,
                oppgave.beskrivelse,
            )
        }
    }

    fun finnBehandleSakOppgaver(
        opprettetTomTidspunktPåBehandleSakOppgave: LocalDateTime,
    ): List<FinnOppgaveResponseDto> {
        val limit: Long = 2000

        val behandleSakOppgaver =
            oppgaveClient.hentOppgaver(
                finnOppgaveRequest =
                    FinnOppgaveRequest(
                        tema = Tema.ENF,
                        oppgavetype = BehandleSak,
                        limit = limit,
                        opprettetTomTidspunkt = opprettetTomTidspunktPåBehandleSakOppgave,
                    ),
            )

        val behandleUnderkjent =
            oppgaveClient.hentOppgaver(
                finnOppgaveRequest =
                    FinnOppgaveRequest(
                        tema = Tema.ENF,
                        oppgavetype = BehandleUnderkjentVedtak,
                        limit = limit,
                    ),
            )

        val godkjenne =
            oppgaveClient.hentOppgaver(
                finnOppgaveRequest =
                    FinnOppgaveRequest(
                        tema = Tema.ENF,
                        oppgavetype = GodkjenneVedtak,
                        limit = limit,
                    ),
            )

        logger.info("Hentet oppgaver:  ${behandleSakOppgaver.antallTreffTotalt}, ${behandleUnderkjent.antallTreffTotalt}, ${godkjenne.antallTreffTotalt}")

        feilHvis(behandleSakOppgaver.antallTreffTotalt >= limit) { "For mange behandleSakOppgaver - limit truffet: + $limit " }
        feilHvis(behandleUnderkjent.antallTreffTotalt >= limit) { "For mange behandleUnderkjent - limit truffet: + $limit " }
        feilHvis(godkjenne.antallTreffTotalt >= limit) { "For mange godkjenne - limit truffet: + $limit " }

        return listOf(behandleSakOppgaver, behandleUnderkjent, godkjenne)
    }

    fun finnBehandlingsoppgaveSistEndretIEFSak(behandlingId: UUID): Oppgave? {
        val oppgaveSistEndret =
            oppgaveRepository
                .findByBehandlingIdAndTypeIn(
                    behandlingId,
                    setOf(BehandleSak, GodkjenneVedtak, BehandleUnderkjentVedtak),
                ).oppgaveSistEndret()

        return oppgaveSistEndret?.let {
            hentOppgave(it.gsakOppgaveId)
        }
    }

    fun finnVurderHenvendelseOppgaver(behandlingId: UUID): List<VurderHenvendelseOppgaveDto> {
        val vurderHenvendelsOppgave =
            oppgaveRepository.findByBehandlingIdAndType(behandlingId, VurderHenvendelse)
        val oppgaveListe = vurderHenvendelsOppgave?.filter { it.oppgaveSubtype != null } ?: emptyList()

        return oppgaveListe.map {
            VurderHenvendelseOppgaveDto(
                it.oppgaveSubtype
                    ?: error("VurderHenvendelseOppgavetype på Oppgave skal ikke kunne være null"),
                it.sporbar.opprettetTid.toLocalDate(),
            )
        }
    }

    private fun utledSettBehandlesAvApplikasjon(oppgavetype: Oppgavetype) =
        when (oppgavetype) {
            BehandleSak,
            BehandleUnderkjentVedtak,
            GodkjenneVedtak,
            -> true

            InnhentDokumentasjon -> false
            VurderHenvendelse -> false
            else -> error("Håndterer ikke behandlesAvApplikasjon for $oppgavetype")
        }

    private fun List<no.nav.familie.ef.sak.oppgave.Oppgave>?.oppgaveSistEndret(): no.nav.familie.ef.sak.oppgave.Oppgave? = this?.sortedBy { it.sistEndret() }?.last()

    private fun no.nav.familie.ef.sak.oppgave.Oppgave.sistEndret(): LocalDateTime = this.sporbar.endret.endretTid
}
