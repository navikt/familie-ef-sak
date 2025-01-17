package no.nav.familie.ef.sak.kontantstøtte

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling/kontantstotte"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KontantstøtteController(
    private val kontantstøtteService: KontantstøtteService,
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
) {
    @GetMapping("{behandlingId}/finnesUtbetalinger")
    fun finnesKontantstøtteUtbetalinger(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<HentUtbetalingsinfoKontantstøtteDto> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        }
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        return Ressurs.success(kontantstøtteService.hentUtbetalingsinfoKontantstøtte(personIdent))
    }
}
