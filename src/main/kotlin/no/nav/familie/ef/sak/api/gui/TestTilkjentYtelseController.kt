package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.integration.ØkonomiKlient
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.YtelseType
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping(path = ["/api/test/"])
@ProtectedWithClaims(issuer = "azuread")
class TestTilkjentYtelseController(private val tilkjentYtelseService: TilkjentYtelseService, private val økonomiKlient: ØkonomiKlient) {

    @PostMapping("/send-til-oppdrag")
    fun testMotOppdrag(@RequestBody tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): Ressurs<TilkjentYtelse> {
        val nyTilkjentYtelse = tilkjentYtelseTestDTO.nyTilkjentYtelse
        val forrigeTilkjentYtelse = tilkjentYtelseTestDTO.forrigeTilkjentYtelse

        val tilkjentYtelseMedUtbetalingsoppdrag = UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                nyTilkjentYtelse = nyTilkjentYtelse,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse)

        økonomiKlient.iverksettOppdrag(tilkjentYtelseMedUtbetalingsoppdrag.utbetalingsoppdrag!!)
        return Ressurs.success(tilkjentYtelseMedUtbetalingsoppdrag)
    }

    @GetMapping("/dummy")
    fun dummyTilkjentYtelse(@RequestParam(defaultValue = "12345678911") fnr: String, @RequestParam(defaultValue = "103242") behandlingEksternId: Long): Ressurs<TilkjentYtelse> {
        val søker = fnr
        val andelTilkjentYtelseDto = AndelTilkjentYtelseDTO(personIdent = søker, beløp = 1000, stønadFom = LocalDate.now(), stønadTom = LocalDate.now(), type = YtelseType.OVERGANGSSTØNAD)
        val tilkjentYtelseDto = TilkjentYtelseDTO(søker = søker, saksnummer = "12345", behandlingId = UUID.randomUUID(), andelerTilkjentYtelse = listOf(andelTilkjentYtelseDto, andelTilkjentYtelseDto))
        return Ressurs.success(tilkjentYtelseDto.tilTilkjentYtelse(behandlingEksternId))
    }

}