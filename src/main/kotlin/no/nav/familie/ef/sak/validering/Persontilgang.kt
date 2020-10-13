package no.nav.familie.ef.sak.validering

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.service.PersonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Persontilgang(private val integrasjonerClient: FamilieIntegrasjonerClient,
                    private val personService: PersonService)
    : ConstraintValidator<PersontilgangConstraint, PersonIdentDto> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(personIdent: PersonIdentDto, ctx: ConstraintValidatorContext): Boolean {
        val identifikatorer = personService.hentPersonMedRelasjoner(personIdent.personIdent).identifikatorer()
        integrasjonerClient.sjekkTilgangTilPersoner(identifikatorer)
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang")
                    return false
                }

        return true
    }

}
