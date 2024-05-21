package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.dto.FørstegangsbehandlingDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/forstegangsbehandling"])
@ProtectedWithClaims(issuer = "azuread")
class FørstegangsbehandlingController(
    private val førstegangsbehandlingService: FørstegangsbehandlingService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("{fagsakId}/opprett")
    fun opprettFørstegangsbehandlingManuelt(
        @PathVariable fagsakId: UUID,
        @RequestBody førstegangsBehandlingRequest: FørstegangsbehandlingDto,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()

        val behandling = førstegangsbehandlingService.opprettFørstegangsbehandling(fagsakId, førstegangsBehandlingRequest)
        return Ressurs.success(behandling.id)
    }
}
