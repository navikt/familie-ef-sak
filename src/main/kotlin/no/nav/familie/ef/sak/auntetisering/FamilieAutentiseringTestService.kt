package no.nav.familie.ef.sak.auntetisering

import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FamilieAutentiseringTestService(
    private val tilgangService: TilgangService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun testRolleValideringMotToken(behandlerRolle: BehandlerRolle, header: String?) {
        secureLogger.info("Header motatt fra familie-ef-autentisering: $header")
        secureLogger.info("Mottok kall fra familie-ef-autentisering med rolle: $behandlerRolle")
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        secureLogger.info("Claims er: $claims")
        secureLogger.info("Issuer for token er: ${claims.issuer}.")
        secureLogger.info("Claims funnet i Texas token er: ${claims.allClaims}. Går videre mot validering av rolle.")
        tilgangService.validerTilgangTilRolle(behandlerRolle)
    }
}
