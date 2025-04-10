package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/autentisering")
@ProtectedWithClaims(issuer = "azuread")
class FamilieAutentiseringTestController(
    private val tilgangService: TilgangService,
) {
    @PostMapping
    @RequestMapping("test-token-validering")
    @ProtectedWithClaims(issuer = "azuread")
    @Profile("!prod")
    fun testRolleValidering(
        @RequestBody behandlerRolle: BehandlerRolle,
    ): String {
        tilgangService.validerTilgangTilRolle(behandlerRolle)
        return "ok"
    }
}
