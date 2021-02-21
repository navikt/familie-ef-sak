package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.Søkeresultat
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/sok"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøkController(private val fagsakService: FagsakService, private val tilgangService: TilgangService) {

    @PostMapping()
    fun sokPerson(@RequestBody personIdentRequest: PersonIdentDto): Ressurs<Søkeresultat> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdentRequest.personIdent)
        return Ressurs.success(fagsakService.soekPerson(personIdentRequest.personIdent))
    }
}