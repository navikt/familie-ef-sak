package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/tilkjentytelse"])
@ProtectedWithClaims(issuer = "azuread")
class TilkjentYtelseController(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val andelsHistorikkService: AndelsHistorikkService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/behandling/{behandlingId}")
    fun hentTilkjentYtelseForBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<TilkjentYtelseDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(tilkjentYtelseService.hentForBehandling(behandlingId).tilDto())
    }

    @GetMapping("/barn/{behandlingId}")
    fun hentBarnMedLøpendeUtbetalinger(
        @PathVariable behandlingId: UUID,
    ): Ressurs<BarnMedLøpendeStønad> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(andelsHistorikkService.utledLøpendeUtbetalingForBarnIBarnetilsyn(behandlingId))
    }
}
