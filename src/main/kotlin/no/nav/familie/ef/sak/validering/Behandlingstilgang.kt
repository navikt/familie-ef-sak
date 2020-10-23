package no.nav.familie.ef.sak.validering

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.repository.SøknadRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

@Component
class Behandlingstilgang(private val søknadRepository: SøknadRepository,
                         private val integrasjonerClient: FamilieIntegrasjonerClient)
    : ConstraintValidator<BehandlingConstraint, UUID> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun isValid(behandlingId: UUID, ctx: ConstraintValidatorContext): Boolean = harTilgang(behandlingId)

    fun validerTilgang(behandlingId: UUID) {
        if (!harTilgang(behandlingId)) {
            throw Feil(message = "Har ikke tilgang til saken",
                       frontendFeilmelding = "Har ikke tilgang til saken",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
    }

    fun harTilgang(behandlingId: UUID): Boolean {
        val søknad = søknadRepository.findByBehandlingId(behandlingId) ?: error("behandling finnes ikke: $behandlingId ")
        val personer = søknad.relaterteFnr + søknad.søker.fødselsnummer
        return harTilgang(personer)
    }

    // TODO gjøre om slik att vi sjekker om man har tilgang på alle barn til søkeren og ikke kun de som er med i søknaden
    fun harTilgang(personer: Set<String>): Boolean {

        integrasjonerClient.sjekkTilgangTilPersoner(personer.toList())
                .filterNot { it.harTilgang }
                .forEach {
                    logger.error("Bruker har ikke tilgang: ${it.begrunnelse}")
                    return false
                }
        return true
    }

}
