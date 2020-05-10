package no.nav.familie.ef.sak.økonomi.dto

import java.lang.IllegalStateException
import java.time.LocalDate

data class TilkjentYtelseRestDTO(
    val søker: String,
    val saksnummer: String,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelseRestDTO>
) {

    fun valider() {
        if(andelerTilkjentYtelse.size==0) {
            throw IllegalStateException("Request trenger minst én andel tilkjent ytelse")
        }

        andelerTilkjentYtelse.forEach {
            if(it.beløp < 0)
                throw IllegalStateException("Beløp på andel tilkjent ytelse er negativt")
            if(it.stønadTom < it.stønadFom)
                throw IllegalStateException("Stønad til-og-med-dato (${it.stønadTom}) " +
                                            "kan ikke være tidligere enn stønad fra-og-med-dato (${it.stønadFom})")
        }
    }
}

data class AndelTilkjentYtelseRestDTO(
        val personIdentifikator : String,
        val beløp: Int,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val type: YtelseType
)

fun TilkjentYtelseRestDTO.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET) : TilkjentYtelse {

    val minStønadFom = this.andelerTilkjentYtelse.map { it.stønadFom }.min()
    val maxStønadTom = this.andelerTilkjentYtelse.map { it.stønadTom }.max()

    return TilkjentYtelse(
            personIdentifikator = søker,
            saksnummer = saksnummer,
            stønadFom = minStønadFom,
            stønadTom = maxStønadTom,
            vedtaksdato = LocalDate.now(),
            status = status
    )
}

fun TilkjentYtelseRestDTO.tilAndelerTilkjentYtelse(tilkjentYtelseId: Long) : List<AndelTilkjentYtelse> {

    return this.andelerTilkjentYtelse
            .map {
                AndelTilkjentYtelse(
                        tilkjentYtelseId = tilkjentYtelseId,
                        personIdentifikator = it.personIdentifikator,
                        beløp = it.beløp,
                        stønadFom = it.stønadFom,
                        stønadTom = it.stønadTom,
                        type = it.type
                )
            }
}

fun TilkjentYtelse.tilDto(andelerTilkjentYtelse: List<AndelTilkjentYtelseRestDTO>) : TilkjentYtelseRestDTO {
    return TilkjentYtelseRestDTO(
            søker = this.personIdentifikator,
            saksnummer = this.saksnummer,
            andelerTilkjentYtelse = andelerTilkjentYtelse
    )
}

fun AndelTilkjentYtelse.tilDto() : AndelTilkjentYtelseRestDTO {
    return AndelTilkjentYtelseRestDTO(
            personIdentifikator = this.personIdentifikator,
            beløp = this.beløp,
            stønadFom = this.stønadFom,
            stønadTom = this.stønadTom,
            type = this.type
    )
}
