package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tekniskopphor") // "/internal/tekniskopphor ``"
@ProtectedWithClaims(issuer = "azuread")

class TekniskOpphørController(private val iverksettService: IverksettService) {

    @PostMapping("{behandlingId}")
    fun iverksettTekniskopphor(@PathVariable behandlingId: String) {
            iverksettService.teknisktOpphor(behandlingId)
    }
}