package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.service.TeknisktOpphørService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tekniskopphor") // "/internal/tekniskopphor ``"
@ProtectedWithClaims(issuer = "azuread")

class TekniskOpphørController(private val teknisktOpphørService: TeknisktOpphørService) {

    @PostMapping
    fun iverksettTekniskopphor(@RequestBody personIdent: PersonIdent) {
        teknisktOpphørService.håndterTeknisktOpphør(personIdent)
    }
}