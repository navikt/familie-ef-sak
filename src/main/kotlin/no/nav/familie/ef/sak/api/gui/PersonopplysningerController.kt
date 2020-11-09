package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.service.PersonopplysningerService
import no.nav.familie.ef.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.ConstraintViolationException


@RestController
@RequestMapping(path = ["/api/personopplysninger/"], produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PersonopplysningerController(private val personopplysningerService: PersonopplysningerService) {

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleRestClientResponseException(e: ConstraintViolationException): Ressurs<ConstraintViolationException> {
        return Ressurs.failure(e.message, null, e)
    }

    @PostMapping
    fun personopplysninger(@PersontilgangConstraint @RequestBody personIdent: PersonIdentDto): Ressurs<PersonopplysningerDto> {
        return Ressurs.success(personopplysningerService.hentPersonopplysninger(personIdent.personIdent))
    }

}
