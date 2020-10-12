package no.nav.familie.ef.sak.validering

import no.nav.familie.ef.sak.api.fagsak.FagsakRequest
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.service.PersonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class FagsakPersontilgang(private val integrasjonerClient: FamilieIntegrasjonerClient,
                          private val personService: PersonService)
    : ConstraintValidator<PersontilgangConstraint, FagsakRequest> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(fagsakRequest: FagsakRequest, ctx: ConstraintValidatorContext): Boolean {
        val identifikatorer = personService.hentPersonMedRelasjoner(fagsakRequest.personIdent).identifikatorer()
        integrasjonerClient.sjekkTilgangTilPersoner(identifikatorer)
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}
