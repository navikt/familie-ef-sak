package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.felles.util.FnrUtil.validerOptionalIdent
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_EGEN_ANSATT
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.familie.ef.sak.oppgave.dto.FinnOppgaveRequestDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveEfDto
import no.nav.familie.ef.sak.oppgave.dto.OppgaveResponseDto
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerDto
import no.nav.familie.ef.sak.oppgave.dto.UtdanningOppgaveDto
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
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(
    private val oppgaveService: OppgaveService,
    private val tilgangService: TilgangService,
    private val personService: PersonService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping(
        path = ["/soek"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentOppgaver(
        @RequestBody finnOppgaveRequest: FinnOppgaveRequestDto,
    ): Ressurs<OppgaveResponseDto> {
        validerOptionalIdent(finnOppgaveRequest.ident)

        val aktørId =
            finnOppgaveRequest.ident
                .takeUnless { it.isNullOrBlank() }
                ?.let {
                    personService
                        .hentAktørIder(it)
                        .identer
                        .first()
                        .ident
                }

        secureLogger.info("AktoerId: $aktørId, Ident: ${finnOppgaveRequest.ident}")
        val oppgaveRepons = oppgaveService.hentOppgaver(finnOppgaveRequest.tilFinnOppgaveRequest(aktørId))
        return Ressurs.success(oppgaveRepons.tilDto())
    }

    @GetMapping("/fremleggsoppgaver/{behandlingId}")
    fun hentFremleggsoppgaver(
        @PathVariable behandlingId: UUID,
    ): Ressurs<OppgaveResponseDto> {
        val oppgaveRespons = oppgaveService.hentFremleggsoppgaver(behandlingId)
        return Ressurs.success(oppgaveRespons.tilDto())
    }

    @GetMapping("/oppgaver-for-ferdigstilling/{behandlingId}")
    fun hentOppgaverForFerdigstilling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<OppgaveResponseDto> {
        val oppgaveIder = oppfølgingsoppgaveService.hentOppgaverForFerdigstillingEllerNull(behandlingId)?.fremleggsoppgaveIderSomSkalFerdigstilles
        secureLogger.info("Oppgave ider for behandlingId: $behandlingId, oppgaveIder: $oppgaveIder")
        val oppgaver = oppgaveService.hentOppgaverMedIder(oppgaveIder)
        val antallTreffTotalt = oppgaver.size.toLong()
        val oppgaveResponse = FinnOppgaveResponseDto(antallTreffTotalt, oppgaver)

        return Ressurs.success(oppgaveResponse.tilDto())
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
    fun hentOppgave(
        @PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long,
    ): Ressurs<OppgaveDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        val efOppgave = oppgaveService.hentEfOppgave(gsakOppgaveId)
        return efOppgave?.let { Ressurs.success(OppgaveDto(it.behandlingId, it.gsakOppgaveId)) }
            ?: Ressurs.funksjonellFeil(
                "Denne oppgaven må behandles i Gosys og Infotrygd",
                "Denne oppgaven må behandles i Gosys og Infotrygd",
            )
    }

    @GetMapping("/oppslag/{gsakOppgaveId}")
    fun hentOppgaveFraGosys(
        @PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long,
    ): Ressurs<OppgaveEfDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(oppgaveService.hentOppgave(gsakOppgaveId).tilDto())
    }

    @Deprecated("Har ikke lenger behov for logging - fjern etter at frontend slutter å kalle på dette endepunktet")
    @GetMapping("{behandlingId}/tilordnet-ressurs")
    fun hentTilordnetRessursForBehandlingId(
        @PathVariable behandlingId: UUID,
    ): Ressurs<String?> {
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        val oppgave = tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)
        val saksbehandlerIdentIOppgaveSystemet = oppgave?.tilordnetRessurs
        if (oppgave != null && saksbehandlerIdentIOppgaveSystemet != saksbehandlerIdent) {
            logger.info(
                "(Eier av behandling/oppgave) " +
                    "Saksbehandler $saksbehandlerIdent er inne i behandling=$behandlingId " +
                    "mens oppgaven=${oppgave.id} er tilordnet $saksbehandlerIdentIOppgaveSystemet " +
                    "sekunderSidenEndret=${OppgaveUtil.sekunderSidenEndret(oppgave)}",
            )
        }
        return Ressurs.success(saksbehandlerIdentIOppgaveSystemet)
    }

    @GetMapping("{behandlingId}/ansvarlig-saksbehandler")
    fun hentAnsvarligSaksbehandlerForBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<SaksbehandlerDto> {
        val oppgave = tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandlingGittStegtype(behandlingId)
        return Ressurs.success(tilordnetRessursService.utledAnsvarligSaksbehandlerForOppgave(behandlingId, oppgave))
    }

    @GetMapping("/behandling/{behandlingId}")
    fun hentOppgaveForBehandlingId(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Oppgave> {
        val oppgave = tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)

        return oppgave?.let { Ressurs.success(it) } ?: throw ApiFeil(
            "Fant ingen åpen oppgave for behandlingen",
            HttpStatus.BAD_REQUEST,
        )
    }

    @GetMapping("/behandling/{behandlingId}/settpavent-oppgavestatus")
    fun hentVurderHenvendelseStatus(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<VurderHenvendelseOppgaveDto>> {
        val status: List<VurderHenvendelseOppgaveDto> = oppgaveService.finnVurderHenvendelseOppgaver(behandlingId)
        return Ressurs.success(status)
    }

    @GetMapping(path = ["/mapper"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentMapper(): Ressurs<List<MappeDto>> {
        val enheter = mutableListOf(ENHET_NR_NAY)
        if (tilgangService.harEgenAnsattRolle()) {
            enheter += ENHET_NR_EGEN_ANSATT
        }
        return Ressurs.success(oppgaveService.finnMapper(enheter = enheter))
    }

    @GetMapping(path = ["/utdanningsuttrekk"])
    fun utdanningsuttrekk(
        @RequestParam frist: LocalDate,
    ): Ressurs<List<UtdanningOppgaveDto>> = Ressurs.success(oppgaveService.finnOppgaverIUtdanningsmappe(frist))
}

private fun FinnOppgaveResponseDto.tilDto(): OppgaveResponseDto {
    val oppgaver =
        oppgaver.map {
            it.tilDto()
        }
    return OppgaveResponseDto(antallTreffTotalt, oppgaver)
}

private fun Oppgave.tilDto(): OppgaveEfDto =
    OppgaveEfDto(
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
