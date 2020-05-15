package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.YtelseType
import no.nav.familie.ef.sak.økonomi.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelseDTO
import java.time.LocalDate
import java.util.*

object DataGenerator {
    private fun tilfeldigFødselsnummer() =  Random().nextInt(Int.MAX_VALUE).toString()
    private fun tilfeldigSaksnummer() = "SAK"+Random().nextInt(Int.MAX_VALUE)

    fun tilfeldigAndelTilkjentYtelse(
            tilkjentYtelseId: Long,
            beløp: Int=Random().nextInt(),
            stønadFom: LocalDate = LocalDate.now(),
            stønadTom: LocalDate = LocalDate.now(),
            type: YtelseType = YtelseType.OVERGANGSSTØNAD
    ) = AndelTilkjentYtelse(
            tilkjentYtelseId = tilkjentYtelseId,
            personIdentifikator = UUID.randomUUID().toString().substring(0, 20),
            beløp = beløp,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            type = type)

    fun flereTilfeldigeAndelerTilkjentYtelse(tilkjentYtelseId: Long, antall: Int): List<AndelTilkjentYtelse> =
        (1 .. antall).map { tilfeldigAndelTilkjentYtelse(tilkjentYtelseId) }.toList()

    fun tilfeldigTilkjentYtelse(
            personIdentifikator: String = tilfeldigFødselsnummer(),
            stønadFom: LocalDate = LocalDate.now(),
            stønadTom: LocalDate = LocalDate.now(),
            saksnummer: String = tilfeldigSaksnummer(),
            vedtaksdato: LocalDate = LocalDate.now()

    ) = TilkjentYtelse(
            personIdentifikator = personIdentifikator,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            saksnummer = saksnummer,
            vedtaksdato = vedtaksdato
    )

    fun tilfeldigTilkjentYtelseDto() : TilkjentYtelseDTO {
        val søker = tilfeldigFødselsnummer()

        return TilkjentYtelseDTO(
            søker = søker,
            saksnummer = tilfeldigSaksnummer(),
            andelerTilkjentYtelse = listOf(
                    AndelTilkjentYtelseDTO(
                            personIdentifikator = søker,
                            beløp = Random().nextInt(100_000),
                            stønadFom = LocalDate.now(),
                            stønadTom = LocalDate.now(),
                            type=YtelseType.OVERGANGSSTØNAD
                    )
            ))

    }

}