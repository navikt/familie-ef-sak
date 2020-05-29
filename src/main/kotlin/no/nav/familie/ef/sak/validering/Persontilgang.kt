package no.nav.familie.ef.sak.validering

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Persontilgang(private val integrasjonerClient: FamilieIntegrasjonerClient)
    : ConstraintValidator<PersontilgangConstraint, PersonIdentDto> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(personIdent: PersonIdentDto, ctx: ConstraintValidatorContext): Boolean {

        val jwtToken = SpringTokenValidationContextHolder().tokenValidationContext.getJwtToken("azure")
        integrasjonerClient.sjekkTilgangTilPersoner(listOf(personIdent.personIdent))
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}
