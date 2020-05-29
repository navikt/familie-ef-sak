package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.YtelseType
import no.nav.familie.ef.sak.økonomi.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelseDTO
import java.time.LocalDate
import java.util.*

object DataGenerator {
    private fun tilfeldigFødselsnummer() = Random().nextInt(Int.MAX_VALUE).toString()
    private fun tilfeldigSaksnummer() = "SAK" + Random().nextInt(Int.MAX_VALUE)

    fun flereTilfeldigeAndelerTilkjentYtelse(antall: Int): List<AndelTilkjentYtelse> =
            (1..antall).map { tilfeldigAndelTilkjentYtelse() }.toList()

    fun tilfeldigAndelTilkjentYtelse(beløp: Int = Random().nextInt(),
                                     stønadFom: LocalDate = LocalDate.now(),
                                     stønadTom: LocalDate = LocalDate.now(),
                                     type: YtelseType = YtelseType.OVERGANGSSTØNAD) =
            AndelTilkjentYtelse(beløp = beløp,
                                stønadFom = stønadFom,
                                stønadTom = stønadTom,
                                type = type)

    fun tilfeldigTilkjentYtelse(antallAndelerTilkjentYteelse: Int = 1) =
            TilkjentYtelse(personident = tilfeldigFødselsnummer(),
                           stønadFom = LocalDate.now(),
                           stønadTom = LocalDate.now(),
                           saksnummer = tilfeldigSaksnummer(),
                           vedtaksdato = LocalDate.now(),
                           periodeIdStart = Math.random().toLong(),
                           andelerTilkjentYtelse = flereTilfeldigeAndelerTilkjentYtelse(antallAndelerTilkjentYteelse))

    fun tilfeldigTilkjentYtelseDto(): TilkjentYtelseDTO {
        val søker = tilfeldigFødselsnummer()

        return TilkjentYtelseDTO(søker = søker,
                                 saksnummer = tilfeldigSaksnummer(),
                                 andelerTilkjentYtelse =
                                 listOf(AndelTilkjentYtelseDTO(beløp = Random().nextInt(100_000),
                                                               stønadFom = LocalDate.now(),
                                                               stønadTom = LocalDate.now(),
                                                               type = YtelseType.OVERGANGSSTØNAD)))
    }

}