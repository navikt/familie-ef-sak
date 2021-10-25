package no.nav.familie.ef.sak.inntekt

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/inntekt")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InntektController(
        private val tilgangService: TilgangService,
        private val inntektService: InntektService
) {

    @GetMapping("fagsak/{fagsakId}")
    fun hentInntekt(@PathVariable("fagsakId") fagsakId: UUID): Map<String, Any> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        return inntektService.hentInntekt(fagsakId = fagsakId)
    }
}
