package no.nav.familie.ef.sak.behandling

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandling/barn"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BarnController(val barnService: BarnService) {

    @GetMapping("{personident}")
    fun hentFnrForRegisterbarnISisteIverksatteBehandling(@PathVariable personIdent: String): Ressurs<List<String>> {
        return Ressurs.success(barnService.hentFnrForRegisterbarnISisteIverksatteBehandling(personIdent))
    }
}