package no.nav.familie.ef.sak.validering

import no.nav.familie.ba.sak.validering.PersontilgangConstraint
import no.nav.familie.ba.sak.validering.SakstilgangConstraint
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Persontilgang(private val integrasjonerClient: FamilieIntegrasjonerClient)
    : ConstraintValidator<PersontilgangConstraint, String> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(personIdent: String, ctx: ConstraintValidatorContext): Boolean {
        integrasjonerClient.sjekkTilgangTilPersoner(listOf(personIdent))
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}
