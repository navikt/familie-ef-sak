package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/simulering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SimuleringController(
        private val tilgangService: TilgangService,
        private val simuleringService: SimuleringService,
) {

    @GetMapping("/{behandlingId}")
    fun simulerForBehandling(@PathVariable behandlingId: UUID): Ressurs<Simuleringsoppsummering> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(simuleringService.simuler(behandlingId))
    }
}
