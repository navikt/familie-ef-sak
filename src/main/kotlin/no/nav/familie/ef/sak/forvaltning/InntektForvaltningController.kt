package no.nav.familie.ef.sak.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/forvaltning/inntektsjekk")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class InntektForvaltningController(
    private val aMeldingInntektClient: AMeldingInntektClient,
) {
    val secureLogger = LoggerFactory.getLogger("secureLogger")
    @Operation(
        description = "Kan brukes til å hente inntekt for person de siste 3 månedene for gitt personident for å sjekke inntektresponsen",
        summary =
            "Henter inntekt for gitt personident",
    )
    @PostMapping("/hent-inntekt")
    fun manueltHentInntektPåPersonIdent(
        @RequestBody personIdent: String,
    ) {
        val inntektResponse = aMeldingInntektClient.hentInntekt(
            personIdent = personIdent,
            månedFom = YearMonth.now(),
            månedTom = YearMonth.now().minusMonths(3),
        )

        secureLogger.info("Hentet inntekt med personident ${personIdent} der response er ${inntektResponse}")
    }
}
