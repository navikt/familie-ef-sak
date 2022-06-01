package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.grunnbelop.GOmregningTaskService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@ProtectedWithClaims(issuer = "azuread")
class ManuellGOmregningController(private val gOmregningTaskService: GOmregningTaskService) {

    @GetMapping(path = ["gomregning"])
    fun opprettGOmregningTasksForBehandlingerMedGammeltGBelop(): Ressurs<Int> {
        val antallTaskerOpprettet = gOmregningTaskService.opprettGOmregningTaskForBehandlingerMedUtdatertG()
        return Ressurs.success(antallTaskerOpprettet)
    }
}
