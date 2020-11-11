package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/tilkjentytelse"])
@ProtectedWithClaims(issuer = "azuread")
class TilkjentYtelseController(private val tilkjentYtelseService: TilkjentYtelseService) {

    @GetMapping("{tilkjentYtelseId}")
    fun hentTilkjentYtelse(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<TilkjentYtelseDTO> {
        val tilkjentYtelseDto = tilkjentYtelseService.hentTilkjentYtelseDto(tilkjentYtelseId)

        return ResponseEntity.ok(tilkjentYtelseDto)
    }

    @GetMapping("{tilkjentYtelseId}/utbetaling")
    fun hentStatusUtbetaling(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<OppdragStatus> {
        val status = tilkjentYtelseService.hentStatus(tilkjentYtelseId)

        return ResponseEntity.ok(status)
    }
}