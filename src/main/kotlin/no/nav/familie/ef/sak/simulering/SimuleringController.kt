package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/simulering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SimuleringController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
) {
    @GetMapping("/{behandlingId}")
    fun simulerForBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Simuleringsoppsummering> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)
        return Ressurs.success(simuleringService.simuler(saksbehandling))
    }

    @GetMapping("/simuleringsresultat-er-endret/{behandlingId}")
    fun erSimuleringsoppsummeringEndret(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        return Ressurs.success(simuleringService.erSimuleringsoppsummeringEndret(saksbehandling))
    }
}
