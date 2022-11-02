package no.nav.familie.ef.sak.no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring.BarnetilsynSatsendringService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
@RequestMapping(path = ["/api/test/barnetilsyn/satsendring"])
class SatsendringBarnetilsynTestController(val barnetilsynSatsendringService: BarnetilsynSatsendringService) {

    @GetMapping
    fun finnKanditater() {
        barnetilsynSatsendringService.logFagsakerSomSkalSatsendresMedNySats()
    }
}
