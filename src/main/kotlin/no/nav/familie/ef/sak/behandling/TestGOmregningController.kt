package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.grunnbelop.GOmregningService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestGOmregningController(private val gOmregningService: GOmregningService) {

    @GetMapping(path = ["gomregning"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettGOmregningTasksForBehandlingerMedGammeltGBelop(): Ressurs<Int> {
        val antallTaskerOpprettet = gOmregningService.opprettGOmregningTaskForBehandlingerMedUtdatertG()
        return Ressurs.success(antallTaskerOpprettet)
    }
}