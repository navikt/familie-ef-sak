package no.nav.familie.ef.sak.validering

import no.nav.familie.ba.sak.validering.SakstilgangConstraint
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.service.SakService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Sakstilgang(private val sakRepository: SakService,
                  private val integrasjonerClient: FamilieIntegrasjonerClient)
    : ConstraintValidator<SakstilgangConstraint, UUID> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(sakId: UUID, ctx: ConstraintValidatorContext): Boolean {

        val sak = sakRepository.hentSak(sakId)

        val barn = sak.søknad.folkeregisterbarn?.verdi?.map { it.fødselsnummer.verdi.verdi  } ?: emptyList()

        val personer = barn + sak.søknad.personalia.verdi.fødselsnummer.verdi.verdi

        integrasjonerClient.sjekkTilgangTilPersoner(personer)
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }

        return true
    }

}
