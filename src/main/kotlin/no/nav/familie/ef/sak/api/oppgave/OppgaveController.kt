package no.nav.familie.ef.sak.api.oppgave

import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(private val oppgaveService: OppgaveService,
                        private val tilgangService: TilgangService,
                        private val pdlClient: PdlClient) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @PostMapping(path = ["/soek"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentOppgaver(@RequestBody finnOppgaveRequest: FinnOppgaveRequestDto): Ressurs<FinnOppgaveResponseDto> {

        val aktørId = finnOppgaveRequest.ident.takeUnless { it.isNullOrBlank() }
                ?.let { pdlClient.hentAktørIder(it).identer.first().ident }

        secureLogger.info("AktoerId: ${aktørId}, Ident: ${finnOppgaveRequest.ident}")
        return Ressurs.success(oppgaveService.hentOppgaver(finnOppgaveRequest.tilFinnOppgaveRequest(aktørId)))
    }

    @PostMapping(path = ["/{gsakOppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long,
                      @RequestParam("saksbehandler") saksbehandler: String): Ressurs<Long> {
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(oppgaveService.fordelOppgave(gsakOppgaveId, saksbehandler))
    }

    @PostMapping(path = ["/{gsakOppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long): Ressurs<Long> {
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(oppgaveService.tilbakestillFordelingPåOppgave(gsakOppgaveId))
    }

    @GetMapping(path = ["/{gsakOppgaveId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long): Ressurs<OppgaveDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        val efOppgave = oppgaveService.hentEfOppgave(gsakOppgaveId)
        return efOppgave?.let { Ressurs.success(OppgaveDto(it.behandlingId, it.gsakOppgaveId)) }
               ?: Ressurs.funksjonellFeil("Denne oppgaven må behandles i Gosys og Infotrygd",
                                          "Denne oppgaven må behandles i Gosys og Infotrygd")
    }


}


