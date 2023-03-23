package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.felles.util.FnrUtil.validerOptionalIdent
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_EGEN_ANSATT
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.sekunderSidenEndret
import no.nav.familie.ef.sak.oppgave.dto.FinnOppgaveRequestDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveEfDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveResponseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(
    private val oppgaveService: OppgaveService,
    private val tilgangService: TilgangService,
    private val personService: PersonService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping(
        path = ["/soek"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentOppgaver(@RequestBody finnOppgaveRequest: FinnOppgaveRequestDto): Ressurs<OppgaveResponseDto> {
        validerOptionalIdent(finnOppgaveRequest.ident)

        val aktørId = finnOppgaveRequest.ident.takeUnless { it.isNullOrBlank() }
            ?.let { personService.hentAktørIder(it).identer.first().ident }

        secureLogger.info("AktoerId: $aktørId, Ident: ${finnOppgaveRequest.ident}")
        val oppgaveRepons = oppgaveService.hentOppgaver(finnOppgaveRequest.tilFinnOppgaveRequest(aktørId))
        return Ressurs.success(oppgaveRepons.tilDto())
    }

    @PostMapping(path = ["/{gsakOppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(
        @PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long,
        @RequestParam("saksbehandler") saksbehandler: String,
        @RequestParam("versjon") versjon: Int?,
    ): Ressurs<Long> {
        tilgangService.validerHarSaksbehandlerrolle()
        if (!tilgangService.validerSaksbehandler(saksbehandler)) {
            throw ApiFeil("Kunne ikke validere saksbehandler : $saksbehandler", HttpStatus.BAD_REQUEST)
        }
        return Ressurs.success(oppgaveService.fordelOppgave(gsakOppgaveId, saksbehandler, versjon))
    }

    @PostMapping(path = ["/{gsakOppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(
        @PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long,
        @RequestParam(name = "versjon") versjon: Int?,
    ): Ressurs<Long> {
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(oppgaveService.tilbakestillFordelingPåOppgave(gsakOppgaveId, versjon))
    }

    @GetMapping(path = ["/{gsakOppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long): Ressurs<OppgaveDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        val efOppgave = oppgaveService.hentEfOppgave(gsakOppgaveId)
        return efOppgave?.let { Ressurs.success(OppgaveDto(it.behandlingId, it.gsakOppgaveId)) }
            ?: Ressurs.funksjonellFeil(
                "Denne oppgaven må behandles i Gosys og Infotrygd",
                "Denne oppgaven må behandles i Gosys og Infotrygd",
            )
    }

    @GetMapping("/oppslag/{gsakOppgaveId}")
    fun hentOppgaveFraGosys(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long): Ressurs<OppgaveEfDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(oppgaveService.hentOppgave(gsakOppgaveId).tilDto())
    }

    @GetMapping("{behandlingId}/tilordnet-ressurs")
    fun hentTilordnetRessursForBehandlingId(@PathVariable behandlingId: UUID): Ressurs<String?> {
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        val oppgave = oppgaveService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)
        val saksbehandlerIdentIOppgaveSystemet = oppgave?.tilordnetRessurs
        if (oppgave != null && saksbehandlerIdentIOppgaveSystemet != saksbehandlerIdent) {
            logger.info(
                "(Eier av behandling/oppgave) " +
                    "Saksbehandler $saksbehandlerIdent er inne i behandling=$behandlingId " +
                    "mens oppgaven=${oppgave.id} er tilordnet $saksbehandlerIdentIOppgaveSystemet " +
                    "sekunderSidenEndret=${sekunderSidenEndret(oppgave)}",
            )
        }
        throw ApiFeil("Mangler oppgave", HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/behandling/{behandlingId}")
    fun hentOppgaveForBehandlingId(@PathVariable behandlingId: UUID): Ressurs<Oppgave> {
        val oppgave = oppgaveService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)

        return oppgave?.let { Ressurs.success(it) } ?: throw ApiFeil(
            "Fant ingen åpen oppgave for behandlingen",
            HttpStatus.BAD_REQUEST,
        )
    }

    @GetMapping(path = ["/mapper"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMapper(): Ressurs<List<MappeDto>> {
        val enheter = mutableListOf(ENHET_NR_NAY)
        if (tilgangService.harEgenAnsattRolle()) {
            enheter += ENHET_NR_EGEN_ANSATT
        }
        return Ressurs.success(oppgaveService.finnMapper(enheter = enheter))
    }
}

private fun FinnOppgaveResponseDto.tilDto(): OppgaveResponseDto {
    val oppgaver = oppgaver.map {
        it.tilDto()
    }
    return OppgaveResponseDto(antallTreffTotalt, oppgaver)
}

private fun Oppgave.tilDto(): OppgaveEfDto {
    return OppgaveEfDto(
        id = id,
        identer = identer,
        tildeltEnhetsnr = tildeltEnhetsnr,
        endretAvEnhetsnr = endretAvEnhetsnr,
        opprettetAvEnhetsnr = opprettetAvEnhetsnr,
        journalpostId = journalpostId,
        journalpostkilde = journalpostkilde,
        behandlesAvApplikasjon = behandlesAvApplikasjon ?: "",
        saksreferanse = saksreferanse,
        bnr = bnr,
        samhandlernr = samhandlernr,
        aktoerId = aktoerId,
        orgnr = orgnr,
        tilordnetRessurs = tilordnetRessurs,
        beskrivelse = beskrivelse,
        temagruppe = temagruppe,
        tema = tema,
        behandlingstema = behandlingstema,
        oppgavetype = oppgavetype,
        behandlingstype = behandlingstype,
        versjon = versjon,
        mappeId = mappeId,
        fristFerdigstillelse = fristFerdigstillelse,
        aktivDato = aktivDato,
        opprettetTidspunkt = opprettetTidspunkt,
        opprettetAv = opprettetAv,
        endretAv = endretAv,
        ferdigstiltTidspunkt = ferdigstiltTidspunkt,
        endretTidspunkt = endretTidspunkt,
        prioritet = prioritet,
        status = status,
    )
}
