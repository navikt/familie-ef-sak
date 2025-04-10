package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/autentisering/")
@ProtectedWithClaims(issuer = "azuread")
class FamilieAutentiseringTestController(
    private val tilgangService: TilgangService,
) {
    @PostMapping
    @RequestMapping("test-token-validering)")
    @ProtectedWithClaims(issuer = "azuread")
    fun testTokenValidering() {
        val harTilgang = tilgangService.validerHarForvalterrolle()
        logger.info("Har tilgang: $harTilgang")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
