package no.nav.familie.ef.sak.api

import no.nav.familie.ba.sak.validering.PersontilgangConstraint
import no.nav.familie.ef.sak.api.dto.Person
import no.nav.familie.ef.sak.integration.dto.pdl.PdlHentPersonResponse
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException


@RestController
@RequestMapping(path = ["/api/personinfo"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonInfoController(private val personService: PersonService) {

    @ExceptionHandler(HttpClientErrorException.NotFound::class)
    fun handleRestClientResponseException(e: HttpClientErrorException.NotFound): ResponseEntity<String> {
        return ResponseEntity.status(e.rawStatusCode).body("Feil mot personopplysning. Message=${e.message}")
    }

    @GetMapping
    fun personinfo(@RequestHeader(name = "Nav-Personident") @PersontilgangConstraint ident: String): Ressurs<Person> {
        return Ressurs.success(personService.hentPerson(ident))
    }

    @GetMapping("/pdl")
    fun personinfoPdl(@RequestHeader(name = "Nav-Personident") @PersontilgangConstraint ident: String): PdlHentPersonResponse {
        return personService.hentPdlPerson(ident)
    }
}
