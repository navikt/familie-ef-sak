package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/test/"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestTilkjentYtelseController(private val testTilkjentYtelseService: TestTilkjentYtelseService) {

    @PostMapping("/send-til-oppdrag")
    fun testMotOppdrag(@RequestBody tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): Ressurs<TilkjentYtelse> {

        return Ressurs.success(testTilkjentYtelseService.lagreTilkjentYtelseOgIverksettUtbetaling(tilkjentYtelseTestDTO))
    }

    @PostMapping("/dummy")
    fun dummyTilkjentYtelse(@RequestBody dummyIverksettingDTO: DummyIverksettingDTO?): Ressurs<TilkjentYtelse> {
        val dummyDTO = dummyIverksettingDTO ?: DummyIverksettingDTO()
        val søker = dummyDTO.personIdent
        val andelTilkjentYtelseDto = AndelTilkjentYtelseDTO(personIdent = søker,
                                                            beløp = dummyDTO.beløp,
                                                            stønadFom = dummyDTO.stønadFom,
                                                            opprinnelsesbehandlingId = UUID.randomUUID(),
                                                            stønadTom = dummyDTO.stønadTom)
        val tilkjentYtelseDto = TilkjentYtelseDTO(søker = søker,
                                                  behandlingId = UUID.randomUUID(),
                                                  andelerTilkjentYtelse = listOf(andelTilkjentYtelseDto, andelTilkjentYtelseDto))
        return Ressurs.success(tilkjentYtelseDto.tilTilkjentYtelse())
    }

}