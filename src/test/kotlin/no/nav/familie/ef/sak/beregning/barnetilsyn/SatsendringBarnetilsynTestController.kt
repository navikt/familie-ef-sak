package no.nav.familie.ef.sak.no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring.BarnetilsynSatsendringService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/test/barnetilsyn/satsendring"])
@ProtectedWithClaims(issuer = "azuread")
class SatsendringBarnetilsynTestController(
    private val barnetilsynSatsendringService: BarnetilsynSatsendringService,
    private val tilgangService: TilgangService,
) {
    @GetMapping
    fun finnKanditater() {
        tilgangService.validerHarForvalterrolle()
        barnetilsynSatsendringService.finnFagsakerSomSkalSatsendresMedNySatsDersomBaselineErOk()
    }
}
