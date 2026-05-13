package no.nav.familie.ef.sak.ekstern.bisys

import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysRequest
import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/ekstern/bisys"])
class BisysBarnetilsynController(
    val bisysBarnetilsynService: BisysBarnetilsynService,
) {
    @PostMapping("/perioder-barnetilsyn")
    fun hentPerioderBarnetilsyn(
        @RequestBody barnetilsynBisysRequest: BarnetilsynBisysRequest,
    ): BarnetilsynBisysResponse =
        bisysBarnetilsynService.hentBarnetilsynperioderFraEfOgInfotrygd(
            barnetilsynBisysRequest.ident,
            barnetilsynBisysRequest.fomDato,
        )
}
