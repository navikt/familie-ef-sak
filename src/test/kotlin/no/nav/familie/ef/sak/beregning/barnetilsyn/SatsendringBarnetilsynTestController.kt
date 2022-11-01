package no.nav.familie.ef.sak.no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring.BarnetilsynSatsendringService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/test/barnetilsyn/satsendring"])
@Unprotected
class SatsendringBarnetilsynTestController(val barnetilsynSatsendringService: BarnetilsynSatsendringService) {

    fun opprettTasks() {
    }

    fun finnKanditater() {
        barnetilsynSatsendringService.kjørSatsendring()
    }

    fun kjørRevurdering() {
    }
}
