package no.nav.familie.ef.sak.api.oppgave

import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.util.RessursUtils.ok
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(val oppgaveService: OppgaveService) {

    @PostMapping(path = ["/hent-oppgaver"],
                 consumes = [MediaType.APPLICATION_JSON_VALUE],
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    fun hentOppgaver(@RequestBody finnOppgaveRequest: FinnOppgaveRequestDto): ResponseEntity<Ressurs<FinnOppgaveResponseDto>> {
        return ok(oppgaveService.hentOppgaver(finnOppgaveRequest.tilFinnOppgaveRequest()))
    }


    @PostMapping(path = ["/{gsakOppgaveId}/fordel"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun fordelOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long,
                      @RequestParam("saksbehandler") saksbehandler: String
    ): ResponseEntity<Ressurs<Long>> {
        return ok(oppgaveService.fordelOppgave(gsakOppgaveId, saksbehandler))
    }

    @PostMapping(path = ["/{gsakOppgaveId}/tilbakestill"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun tilbakestillFordelingPåOppgave(@PathVariable(name = "gsakOppgaveId") gsakOppgaveId: Long): ResponseEntity<Ressurs<Long>> {
        return ok(oppgaveService.tilbakestillFordelingPåOppgave(gsakOppgaveId))
    }

}