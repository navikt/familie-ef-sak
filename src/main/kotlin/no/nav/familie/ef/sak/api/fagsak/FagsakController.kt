package no.nav.familie.ef.sak.api.fagsak

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService) {

    @PostMapping("/{stønadstype}")
    fun hentFagsakForPerson(@PathVariable("stønadstype") stønadstype: Stønadstype,
                            @RequestBody personIdent: String): Ressurs<FagsakDto> {
        // TODO: Sjekk at personen eksisterer
        // TODO: Sjekk "tilgangskontroll" (kode6, 7, egenAnsatt) for personen
        return Ressurs.success(fagsakService.hentEllerOpprettFagsak(personIdent, stønadstype))
    }

}