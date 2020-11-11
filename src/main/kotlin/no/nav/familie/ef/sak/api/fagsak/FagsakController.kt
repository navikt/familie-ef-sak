package no.nav.familie.ef.sak.api.fagsak

import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService, private val tilgangService: TilgangService) {

    @PostMapping
    fun hentFagsakForPerson(@RequestBody fagsakRequest: FagsakRequest): Ressurs<FagsakDto> {
        tilgangService.validerTilgangTilPersonMedBarn(fagsakRequest.personIdent)
        return Ressurs.success(fagsakService.hentEllerOpprettFagsak(fagsakRequest.personIdent, fagsakRequest.stønadstype))
    }

}