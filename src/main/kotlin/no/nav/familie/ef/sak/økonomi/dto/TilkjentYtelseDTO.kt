package no.nav.familie.ef.sak.økonomi.dto

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.økonomi.domain.YtelseType
import java.time.LocalDate

data class TilkjentYtelseDTO(
    val søker: String,
    val saksnummer: String,
    val andelerTilkjentYtelse: List<AndelTilkjentYtelseDTO>
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

data class AndelTilkjentYtelseDTO(
        val personIdentifikator : String,
        val beløp: Int,
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val type: YtelseType
)

fun TilkjentYtelseDTO.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET) : TilkjentYtelse {

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

fun TilkjentYtelseDTO.tilAndelerTilkjentYtelse(tilkjentYtelseId: Long) : List<AndelTilkjentYtelse> {

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

fun TilkjentYtelse.tilDto(andelerTilkjentYtelse: List<AndelTilkjentYtelseDTO>) : TilkjentYtelseDTO {
    return TilkjentYtelseDTO(
            søker = this.personIdentifikator,
            saksnummer = this.saksnummer,
            andelerTilkjentYtelse = andelerTilkjentYtelse
    )
}

fun AndelTilkjentYtelse.tilDto() : AndelTilkjentYtelseDTO {
    return AndelTilkjentYtelseDTO(
            personIdentifikator = this.personIdentifikator,
            beløp = this.beløp,
            stønadFom = this.stønadFom,
            stønadTom = this.stønadTom,
            type = this.type
    )
}
