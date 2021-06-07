package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/test/"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestTilkjentYtelseController(
    private val testTilkjentYtelseService: TestTilkjentYtelseService,
    private val avstemmingService: AvstemmingService
) {

    @PostMapping("/send-til-oppdrag")
    fun testMotOppdrag(@RequestBody tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): Ressurs<TilkjentYtelse> {

        return Ressurs.success(testTilkjentYtelseService.lagreTilkjentYtelseOgIverksettUtbetaling(tilkjentYtelseTestDTO))
    }

    @GetMapping("/konsistensavstemming/{stønadstype}")
    fun getKonsistensavstemming(@PathVariable stønadstype: Stønadstype): Ressurs<KonsistensavstemmingRequestV2> {
        return Ressurs.success(testTilkjentYtelseService.konsistensavstemOppdrag(stønadstype))
    }

    @PostMapping("/konsistensavstemming/{stønadstype}")
    fun konsistensavstemming(@PathVariable stønadstype: Stønadstype): Ressurs<String> {
        avstemmingService.konsistensavstemOppdrag(stønadstype)
        return Ressurs.success("ok")
    }

    @PostMapping("/dummy")
    fun dummyTilkjentYtelse(@RequestBody dummyIverksettingDTO: DummyIverksettingDTO?): Ressurs<TilkjentYtelseDTO> {
        val dummyDTO = dummyIverksettingDTO ?: DummyIverksettingDTO()
        val søker = dummyDTO.personIdent
        val andelTilkjentYtelseDto = AndelTilkjentYtelseDTO(personIdent = søker,
                                                            beløp = dummyDTO.beløp,
                                                            stønadFom = dummyDTO.stønadFom,
                                                            kildeBehandlingId = UUID.randomUUID(),
                                                            inntektsreduksjon = 0,
                                                            samordningsfradrag = 0,
                                                            inntekt = 0,
                                                            stønadTom = dummyDTO.stønadTom)
        val tilkjentYtelseDto = TilkjentYtelseDTO(søker = søker,
                                                  behandlingId = UUID.randomUUID(),
                                                  andelerTilkjentYtelse = listOf(andelTilkjentYtelseDto, andelTilkjentYtelseDto))
        return Ressurs.success(tilkjentYtelseDto)
    }

}