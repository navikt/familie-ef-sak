package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.service.TekniskOpphørService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/tekniskopphor")
@ProtectedWithClaims(issuer = "azuread")

class TekniskOpphørController(val tekniskOpphørService: TekniskOpphørService, val tilgangService: TilgangService) {

    @PostMapping("{fagsakId}")
    fun iverksettTekniskopphor(@PathVariable fagsakId: UUID): Ressurs<String> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        tilgangService.validerHarBeslutterrolle()
        tekniskOpphørService.håndterTeknisktOpphør(fagsakId)
        return Ressurs.success("OK")
    }
}