package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDto
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
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
class TilkjentYtelseController(private val tilkjentYtelseService: TilkjentYtelseService,
                               private val tilgangService: TilgangService) {


    @GetMapping("/behandling/{behandlingId}")
    fun hentTilkjentYtelseForBehandling(@PathVariable behandlingId: UUID): Ressurs<TilkjentYtelseDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(tilkjentYtelseService.hentForBehandling(behandlingId).tilDto())
    }
}