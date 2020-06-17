package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.gui.dto.Person
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.ef.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import javax.validation.ConstraintViolationException


@RestController
@RequestMapping(path = ["/api/personinfo"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonInfoController(private val personService: PersonService) {

    @ExceptionHandler(HttpClientErrorException.NotFound::class)
    fun handleRestClientResponseException(e: HttpClientErrorException.NotFound): ResponseEntity<String> {
        return ResponseEntity.status(e.rawStatusCode).body("Feil mot personopplysning. Message=${e.message}")
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleRestClientResponseException(e: ConstraintViolationException): Ressurs<ConstraintViolationException> {
        return Ressurs.failure(e.message, null, e)
    }

    @PostMapping
    fun personinfo(@PersontilgangConstraint @RequestBody personIdent: PersonIdentDto): Ressurs<Person> {
        return Ressurs.success(personService.hentPerson(personIdent.personIdent))
    }

    @GetMapping("/soker") // TODO Testendepunkt. Fjernes etter hvert
    fun søkerinfo(@RequestHeader(name = "Nav-Personident") @PersontilgangConstraint ident: String): PdlSøker {
        return personService.hentPdlPerson(ident)
    }

}
