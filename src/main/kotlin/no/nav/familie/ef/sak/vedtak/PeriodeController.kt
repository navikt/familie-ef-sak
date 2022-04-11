package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseDto
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.tilDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/perioder")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class PeriodeController(private val tilgangService: TilgangService,
                        private val tilkjentYtelseService: TilkjentYtelseService) {

    @GetMapping("/{behandlingId}")
    fun hentPerioder(@PathVariable behandlingId: UUID): Ressurs<TilkjentYtelseDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(tilkjentYtelseService.hentForBehandling(behandlingId).tilDto())
    }

    @GetMapping("/fagsak/{fagsakId}/historikk")
    fun hentHistorikk(@PathVariable fagsakId: UUID,
                      @RequestParam tilOgMedBehandlingId: UUID? = null): Ressurs<List<AndelHistorikkDto>> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(tilkjentYtelseService.hentHistorikk(fagsakId, tilOgMedBehandlingId).reversed())
    }
}