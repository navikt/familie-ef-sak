package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.brev.dto.FrittståendeSanitybrevDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/frittstaende-brev"])
@ProtectedWithClaims(issuer = "azuread")
class FrittståendeBrevController(
    private val frittståendeBrevService: FrittståendeBrevService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/{fagsakId}/{brevMal}")
    fun forhåndsvisFrittsåendeSanitybrev(
        @PathVariable fagsakId: UUID,
        @PathVariable brevMal: String,
        @RequestBody brevRequest: JsonNode,
    ): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(frittståendeBrevService.lagFrittståendeSanitybrev(fagsakId, brevMal, brevRequest))
    }

    @PostMapping("/send/{fagsakId}")
    fun sendFrittsåendeSanitybrev(
        @PathVariable fagsakId: UUID,
        @RequestBody sendBrevRequest: FrittståendeSanitybrevDto,
    ): Ressurs<Unit> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(frittståendeBrevService.sendFrittståendeSanitybrev(fagsakId, sendBrevRequest))
    }
}
