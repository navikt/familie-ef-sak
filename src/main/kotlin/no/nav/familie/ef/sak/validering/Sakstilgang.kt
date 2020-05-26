package no.nav.familie.ef.sak.validering

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.repository.SakRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Sakstilgang(private val sakRepository: SakRepository,
                  private val integrasjonerClient: FamilieIntegrasjonerClient)
    : ConstraintValidator<SakstilgangConstraint, UUID> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(sakId: UUID, ctx: ConstraintValidatorContext): Boolean {

        val sak = sakRepository.findByIdOrNull(sakId) ?: error("sak finnes ikke: $sakId ")

        val relatertePersoner =
                sak.barn.map { listOf(it.fødselsnummer, it.annenForelder?.fødselsnummer) }.flatten().filterNotNull()

        val personer = relatertePersoner + sak.søker.fødselsnummer

        integrasjonerClient.sjekkTilgangTilPersoner(personer)
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }
        return true
    }
}
