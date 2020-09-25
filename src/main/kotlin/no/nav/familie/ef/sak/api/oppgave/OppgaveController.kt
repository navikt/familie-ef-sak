package no.nav.familie.ef.sak.api.oppgave

import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(val oppgaveService: OppgaveService) {

    @PostMapping(path = ["/soek"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentOppgaver(@RequestBody finnOppgaveRequest: FinnOppgaveRequestDto): Ressurs<FinnOppgaveResponseDto> {
        return Ressurs.success(oppgaveService.hentOppgaver(finnOppgaveRequest.tilFinnOppgaveRequest()))
    }

    @PostMapping(path = ["/{gsakOppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long,
                      @RequestParam("saksbehandler") saksbehandler: String): Ressurs<Long> {
        return Ressurs.success(oppgaveService.fordelOppgave(gsakOppgaveId, saksbehandler))
    }

    @PostMapping(path = ["/{gsakOppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long): Ressurs<Long> {
        return Ressurs.success(oppgaveService.tilbakestillFordelingPåOppgave(gsakOppgaveId))
    }

}
