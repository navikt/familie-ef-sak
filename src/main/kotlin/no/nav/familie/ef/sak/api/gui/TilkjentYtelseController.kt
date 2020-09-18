package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.*

@RestController
@RequestMapping(path = ["/api/tilkjentytelse"])
@ProtectedWithClaims(issuer = "azuread")
class TilkjentYtelseController(private val tilkjentYtelseService: TilkjentYtelseService) {

    @PostMapping
    fun opprettTilkjentYtelse(@RequestBody tilkjentYtelseDTO: TilkjentYtelseDTO): ResponseEntity<Long> {

        tilkjentYtelseDTO.valider()

        val tilkjentYtelseId = tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseDTO)

        val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(tilkjentYtelseId.toString())
                .build().toUri()

        return ResponseEntity.created(location).build()
    }

    @GetMapping("{tilkjentYtelseId}")
    fun hentTilkjentYtelse(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<TilkjentYtelseDTO> {
        val tilkjentYtelseDto = tilkjentYtelseService.hentTilkjentYtelseDto(tilkjentYtelseId)

        return ResponseEntity.ok(tilkjentYtelseDto)
    }

    @PutMapping("{tilkjentYtelseId}/utbetaling")
    fun sørgForUtbetaling(@PathVariable tilkjentYtelseId: UUID): HttpStatus {
        tilkjentYtelseService.iverksettUtbetalingsoppdrag(tilkjentYtelseId)

        return HttpStatus.ACCEPTED
    }

    @DeleteMapping("{tilkjentYtelseId}/utbetaling")
    fun opphørUtbetaling(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<Long> {
        val opphørtTilkjentYtelseId = tilkjentYtelseService.opphørUtbetalingsoppdrag(tilkjentYtelseId)

        val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(opphørtTilkjentYtelseId.toString())
                .build().toUri()

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(location).build()
    }

    @GetMapping("{tilkjentYtelseId}/utbetaling")
    fun hentStatusUtbetaling(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<OppdragStatus> {
        val status = tilkjentYtelseService.hentStatus(tilkjentYtelseId)

        return ResponseEntity.ok(status)
    }

}