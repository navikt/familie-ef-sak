package no.nav.familie.ef.sak.validering

import no.nav.familie.ba.sak.validering.PersontilgangConstraint
import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Persontilgang2(val persontilgang: Persontilgang) : ConstraintValidator<PersontilgangConstraint, String> {

    override fun isValid(personIdent: String, ctx: ConstraintValidatorContext): Boolean =
            persontilgang.isValid(PersonIdentDto(personIdent), ctx)

}
