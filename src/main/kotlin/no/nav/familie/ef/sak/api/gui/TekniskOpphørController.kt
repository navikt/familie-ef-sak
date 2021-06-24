package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.service.TekniskOpphørService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tekniskopphor")
@ProtectedWithClaims(issuer = "azuread")

class TekniskOpphørController(val tekniskOpphørService: TekniskOpphørService, val tilgangService: TilgangService) {

    @PostMapping
    fun iverksettTekniskopphor(@RequestBody personIdent: PersonIdent): Ressurs<String> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent = personIdent.ident)
        tilgangService.validerHarBeslutterrolle()
        tekniskOpphørService.håndterTeknisktOpphør(personIdent)
        return Ressurs.success("OK")
    }
}