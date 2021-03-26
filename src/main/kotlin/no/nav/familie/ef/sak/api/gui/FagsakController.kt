package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService, private val tilgangService: TilgangService) {

    @PostMapping
    fun hentFagsakForPerson(@RequestBody fagsakRequest: FagsakRequest): Ressurs<FagsakDto> {
        tilgangService.validerTilgangTilPersonMedBarn(fagsakRequest.personIdent)
        return Ressurs.success(fagsakService.hentFagsakMedBehandlinger(fagsakRequest.personIdent, fagsakRequest.stønadstype))
    }

    @GetMapping("{fagsakId}")
    fun hentFagsak(@PathVariable fagsakId: UUID): Ressurs<FagsakDto> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        return Ressurs.success(fagsakService.hentFagsakMedBehandlinger(fagsakId))
    }
}