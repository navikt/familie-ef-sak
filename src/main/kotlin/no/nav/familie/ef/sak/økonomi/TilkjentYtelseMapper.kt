package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.økonomi.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelseDTO
import java.time.LocalDate

fun TilkjentYtelseDTO.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET) : TilkjentYtelse {

    val minStønadFom = this.andelerTilkjentYtelse.map { it.stønadFom }.min()
    val maxStønadTom = this.andelerTilkjentYtelse.map { it.stønadTom }.max()

    return TilkjentYtelse(
            personIdentifikator = søker,
            saksnummer = saksnummer,
            stønadFom = minStønadFom,
            stønadTom = maxStønadTom,
            vedtaksdato = vedtaksdato,
            eksternId = eksternId,
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
