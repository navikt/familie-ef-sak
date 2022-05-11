package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.grunnbelop.GOmregningTaskService
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
class TestGOmregningController(private val gOmregningTaskService: GOmregningTaskService) {

    @GetMapping(path = ["gomregning"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettGOmregningTasksForBehandlingerMedGammeltGBelop(): Ressurs<Int> {
        val antallTaskerOpprettet = gOmregningTaskService.opprettGOmregningTaskForBehandlingerMedUtdatertG()
        return Ressurs.success(antallTaskerOpprettet)
    }
}