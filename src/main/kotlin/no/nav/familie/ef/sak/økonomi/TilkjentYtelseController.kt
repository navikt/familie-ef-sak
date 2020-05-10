package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelseRestDTO
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping(path = ["/api/tilkjentytelse"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TilkjentYtelseController(private val tilkjentYtelseService: TilkjentYtelseService) {

    @PostMapping
    fun opprettTilkjentYtelse(@RequestBody tilkjentYtelseRestDTO: TilkjentYtelseRestDTO): ResponseEntity<Long> {

        tilkjentYtelseRestDTO.valider()

        val tilkjentYtelseId = tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseRestDTO)

        val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path(tilkjentYtelseId.toString())
                .build().toUri()

        return ResponseEntity.created(location).build()
    }

    @GetMapping("{tilkjentYtelseId}")
    fun hentTilkjentYtelse(@PathVariable tilkjentYtelseId: Long): ResponseEntity<TilkjentYtelseRestDTO> {
        val tilkjentYtelseDto = tilkjentYtelseService.hentTilkjentYtelseDto(tilkjentYtelseId)

        return ResponseEntity.ok(tilkjentYtelseDto)
    }

    @PutMapping("{tilkjentYtelseId}/utbetaling")
    fun sørgForUtbetaling(@PathVariable tilkjentYtelseId: Long): HttpStatus {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        tilkjentYtelseService.iverksettUtbetalingsoppdrag(tilkjentYtelseId, saksbehandlerId)

        return HttpStatus.ACCEPTED
    }

    @DeleteMapping("{tilkjentYtelseId}/utbetaling")
    fun opphørUtbetaling(@PathVariable tilkjentYtelseId: Long): HttpStatus {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        tilkjentYtelseService.opphørUtbetalingsoppdrag(tilkjentYtelseId, saksbehandlerId)

        return HttpStatus.ACCEPTED
    }

    @GetMapping("{tilkjentYtelse}/utbetaling")
    fun hentStatusUtbetaling(@PathVariable tilkjentYtelseId: Long): ResponseEntity<OppdragProtokollStatus> {
        val status = tilkjentYtelseService.hentStatus(tilkjentYtelseId)

        return ResponseEntity.ok(status)
    }

}