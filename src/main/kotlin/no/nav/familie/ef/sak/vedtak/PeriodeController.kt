package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDto
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.infrastruktur.tilgang.TilgangService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
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
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(tilkjentYtelseService.hentForBehandling(behandlingId).tilDto())
    }

    @GetMapping("/fagsak/{fagsakId}/historikk")
    fun hentHistorikk(@PathVariable fagsakId: UUID): Ressurs<List<AndelHistorikkDto>> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        return Ressurs.success(tilkjentYtelseService.hentHistorikk(fagsakId).reversed())
    }
}