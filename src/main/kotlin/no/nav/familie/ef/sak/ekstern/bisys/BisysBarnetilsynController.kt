package no.nav.familie.ef.sak.ekstern.bisys

import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysRequest
import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/ekstern/bisys"])
class BisysBarnetilsynController(val bisysBarnetilsynService: BisysBarnetilsynService) {
    @PostMapping("/perioder-barnetilsyn")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentPerioderBarnetilsyn(
        @RequestBody barnetilsynBisysRequest: BarnetilsynBisysRequest,
    ): BarnetilsynBisysResponse {
        return bisysBarnetilsynService.hentBarnetilsynperioderFraEfOgInfotrygd(
            barnetilsynBisysRequest.ident,
            barnetilsynBisysRequest.fomDato,
        )
    }
}
