package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.service.RevurderingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/revurdering"])
@ProtectedWithClaims(issuer = "azuread")
class RevurderingsController(
        private val revurderingService: RevurderingService,
        private val tilgangService: TilgangService,
) {

    @GetMapping("{fagsakId}") // bytt til post
    fun startRevurdering(@PathVariable fagsakId: UUID): Ressurs<String> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        revurderingService.opprettRevurderingManuelt(fagsakId)
        return Ressurs.success("OK")
    }


}