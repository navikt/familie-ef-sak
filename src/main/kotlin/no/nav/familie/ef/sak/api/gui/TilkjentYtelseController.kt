package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/tilkjentytelse"])
@ProtectedWithClaims(issuer = "azuread")
class TilkjentYtelseController(private val tilkjentYtelseService: TilkjentYtelseService,
                               private val behandlingService: BehandlingService,
                               private val tilgangService: TilgangService) {

    @GetMapping("{tilkjentYtelseId}")
    fun hentTilkjentYtelse(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<TilkjentYtelseDTO> {
        val tilkjentYtelseDto = tilkjentYtelseService.hentTilkjentYtelseDto(tilkjentYtelseId)

        return ResponseEntity.ok(tilkjentYtelseDto)
    }

    @GetMapping("{behandlingId}/utbetaling")
    fun hentStatusUtbetaling(@PathVariable behandlingId: UUID): ResponseEntity<OppdragStatus> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val status = tilkjentYtelseService.hentStatus(behandling)
        return ResponseEntity.ok(status)
    }

    @GetMapping("/behandling/{behandlingId}")
    fun hentTilkjentYtelseForBehandling(@PathVariable behandlingId: UUID): Ressurs<TilkjentYtelseDTO> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(tilkjentYtelseService.hentForBehandling(behandlingId).tilDto())
    }
}