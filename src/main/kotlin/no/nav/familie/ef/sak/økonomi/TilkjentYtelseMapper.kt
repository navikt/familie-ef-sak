package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.økonomi.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelseDTO
import java.time.LocalDate

fun TilkjentYtelseDTO.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET): TilkjentYtelse {

    val minStønadFom = this.andelerTilkjentYtelse.map { it.stønadFom }.min() ?: LocalDate.MIN
    val maxStønadTom = this.andelerTilkjentYtelse.map { it.stønadTom }.max() ?: LocalDate.MAX

    return TilkjentYtelse(personident = søker,
                          periodeIdStart = 0L,
                          saksnummer = saksnummer,
                          stønadFom = minStønadFom,
                          stønadTom = maxStønadTom,
                          vedtaksdato = vedtaksdato,
                          status = status,
                          andelerTilkjentYtelse = tilAndelerTilkjentYtelse())
}

fun TilkjentYtelseDTO.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {

    return this.andelerTilkjentYtelse
            .map {
                AndelTilkjentYtelse(beløp = it.beløp,
                                    stønadFom = it.stønadFom,
                                    stønadTom = it.stønadTom,
                                    type = it.type)
            }
}

fun TilkjentYtelse.tilDto(): TilkjentYtelseDTO {
    return TilkjentYtelseDTO(id = this.id,
                             søker = this.personident,
                             saksnummer = this.saksnummer,
                             andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilDto() })
}

fun AndelTilkjentYtelse.tilDto(): AndelTilkjentYtelseDTO {
    return AndelTilkjentYtelseDTO(beløp = this.beløp,
                                  stønadFom = this.stønadFom,
                                  stønadTom = this.stønadTom,
                                  type = this.type)
}

fun TilkjentYtelse.tilOpphør(opphørDato: LocalDate) =
        TilkjentYtelse(type = TilkjentYtelseType.OPPHØR,
                       personident = personident,
                       periodeIdStart = 0L,
                       saksnummer = saksnummer,
                       opphørFom = opphørDato,
                       forrigePeriodeIdStart = periodeIdStart,
                       vedtaksdato = LocalDate.now(),
                       andelerTilkjentYtelse = andelerTilkjentYtelse)
