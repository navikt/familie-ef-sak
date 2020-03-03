package no.nav.familie.ef.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/personinfo"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class PersonInfoController {

    @PostMapping()
    fun sendInn(@RequestBody data: String): Ressurs<String> {
        return Ressurs.success("Dette er en test. Du søkte på:  $data")
    }

}
