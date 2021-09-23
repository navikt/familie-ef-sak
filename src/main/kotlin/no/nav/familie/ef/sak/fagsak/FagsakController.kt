package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.dto.FagsakDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService, private val tilgangService: TilgangService) {

    @PostMapping
    fun hentEllerOpprettFagsakForPerson(@RequestBody fagsakRequest: FagsakRequest): Ressurs<FagsakDto> {
        tilgangService.validerTilgangTilPersonMedBarn(fagsakRequest.personIdent)
        return Ressurs.success(fagsakService.hentEllerOpprettFagsakMedBehandlinger(fagsakRequest.personIdent, fagsakRequest.stønadstype))
    }

    @GetMapping("{fagsakId}")
    fun hentFagsak(@PathVariable fagsakId: UUID): Ressurs<FagsakDto> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        return Ressurs.success(fagsakService.hentFagsakMedBehandlinger(fagsakId))
    }
}