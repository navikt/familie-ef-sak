package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeregningBarnetilsynRequest
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/barnetilsyn/beregning"])
@ProtectedWithClaims(issuer = "azuread")
class BeregningBarnetilsynController(private val beregningBarnetilsynService: BeregningBarnetilsynService) {


    @PostMapping
    fun beregnYtelserForBarnetilsyn(@RequestBody
                                    barnetilsynBeregningRequest: BeregningBarnetilsynRequest): Ressurs<List<BeløpsperiodeBarnetilsynDto>> {

        // TODO valider
        return Ressurs.success(beregningBarnetilsynService.beregnYtelseBarnetilsyn(barnetilsynBeregningRequest.utgiftsperioder,
                                                                                   barnetilsynBeregningRequest.kontantstøtteperioder,
                                                                                   barnetilsynBeregningRequest.tilleggsstønadsperioder))
    }


}
