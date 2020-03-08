package no.nav.familie.ef.sak.api

import no.nav.familie.ef.sak.api.dto.Person
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(path = ["/api/personinfo"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class PersonInfoController(private val personService: PersonService) {

    @PostMapping
    fun personinfo(@RequestHeader(name = "Nav-Personident") ident: String): Person {
        return personService.hentPerson(ident)
    }

}
