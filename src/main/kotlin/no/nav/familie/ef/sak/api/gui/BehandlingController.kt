package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.BehandlingDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(private val behandlingService: BehandlingService,
                           private val tilgangService: TilgangService) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(behandlingService.hentBehandling(behandlingId).tilDto())
    }

}