package no.nav.familie.ef.sak.auntetisering

import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/autentisering")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class FamilieAutentiseringTestController(
    private val familieAutentiseringTestService: FamilieAutentiseringTestService,
) {
    @PostMapping
    @RequestMapping("test-token-validering")
    fun testRolleValidering(
        @RequestBody behandlerRolle: BehandlerRolle,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION) authHeader: String?
    ): String {
        familieAutentiseringTestService.testRolleValideringMotToken(behandlerRolle = behandlerRolle, header = authHeader)
        return "ok"
    }
}
