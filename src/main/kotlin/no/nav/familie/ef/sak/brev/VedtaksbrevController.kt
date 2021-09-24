package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
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
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksbrevController(private val brevService: VedtaksbrevService,
                            private val tilgangService: TilgangService) {

    @GetMapping("/{behandlingId}")
    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(brevService.hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId))
    }

    @PostMapping("/{behandlingId}/{brevMal}")
    fun lagSaksbehandlerbrev(@PathVariable behandlingId: UUID,
                             @PathVariable brevMal: String,
                             @RequestBody brevRequest: JsonNode): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(brevService.lagSaksbehandlerBrev(behandlingId, brevRequest, brevMal))
    }

    @PostMapping("/{behandlingId}")
    fun lagBeslutterbrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        tilgangService.validerHarBeslutterrolle()
        return Ressurs.success(brevService.lagBeslutterBrev(behandlingId))
    }
}
