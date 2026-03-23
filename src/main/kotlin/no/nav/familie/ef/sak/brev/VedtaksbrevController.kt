package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingService
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
import tools.jackson.databind.JsonNode
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksbrevController(
    private val brevService: VedtaksbrevService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/{behandlingId}")
    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(brevService.hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId))
    }

    @PostMapping("/{behandlingId}/{brevMal}")
    fun lagSaksbehandlerbrev(
        @PathVariable behandlingId: UUID,
        @PathVariable brevMal: String,
        @RequestBody brevRequest: JsonNode,
    ): Ressurs<ByteArray> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()
        return Ressurs.success(brevService.lagSaksbehandlerSanitybrev(saksbehandling, brevRequest, brevMal))
    }

    @Deprecated("Slettes - bruk forhåndsvisBeslutterbrev")
    @PostMapping("/{behandlingId}")
    fun lagBeslutterbrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> = foråndsvisBeslutterbrev(behandlingId)

    @PostMapping("/beslutter/vis/{behandlingId}")
    fun forhåndsvisBeslutterbrev(
        @PathVariable behandlingId: UUID,
    ): Ressurs<ByteArray> = foråndsvisBeslutterbrev(behandlingId)

    private fun foråndsvisBeslutterbrev(behandlingId: UUID): Ressurs<ByteArray> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarBeslutterrolle()
        return Ressurs.success(brevService.forhåndsvisBeslutterBrev(saksbehandling))
    }
}
