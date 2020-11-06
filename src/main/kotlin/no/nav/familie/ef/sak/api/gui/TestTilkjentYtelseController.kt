package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.YtelseType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping(path = ["/api/test/"])
@ProtectedWithClaims(issuer = "azuread")
class TestTilkjentYtelseController(private val økonomiClient: OppdragClient) {

    @PostMapping("/send-til-oppdrag")
    fun testMotOppdrag(@RequestBody tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): Ressurs<TilkjentYtelse> {
        val nyTilkjentYtelse = tilkjentYtelseTestDTO.nyTilkjentYtelse
        val forrigeTilkjentYtelse = tilkjentYtelseTestDTO.forrigeTilkjentYtelse

        val tilkjentYtelseMedUtbetalingsoppdrag = UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                nyTilkjentYtelse = nyTilkjentYtelse,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse)

        økonomiClient.iverksettOppdrag(tilkjentYtelseMedUtbetalingsoppdrag.utbetalingsoppdrag!!)
        return Ressurs.success(tilkjentYtelseMedUtbetalingsoppdrag)
    }

    @GetMapping("/dummy")
    fun dummyTilkjentYtelse(@RequestParam(defaultValue = "12345678911") fnr: String): Ressurs<TilkjentYtelse> {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val andelTilkjentYtelseDto = AndelTilkjentYtelseDTO(personIdent = fnr,
                                                            beløp = 1000,
                                                            stønadFom = LocalDate.now(),
                                                            stønadTom = LocalDate.now(),
                                                            type = YtelseType.OVERGANGSSTØNAD)
        val tilkjentYtelseDto = TilkjentYtelseDTO(søker = fnr,
                                                  saksnummer = "12345",
                                                  behandlingId = 54321,
                                                  andelerTilkjentYtelse = listOf(andelTilkjentYtelseDto, andelTilkjentYtelseDto))
        return Ressurs.success(tilkjentYtelseDto.tilTilkjentYtelse(saksbehandler = saksbehandler))
    }

}