package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.repository.domain.*
import java.time.LocalDate

fun TilkjentYtelseDTO.tilTilkjentYtelse(saksbehandler: String,
                                        status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET): TilkjentYtelse {

    val minStønadFom = this.andelerTilkjentYtelse.map { it.stønadFom }.minOrNull() ?: LocalDate.MIN
    val maxStønadTom = this.andelerTilkjentYtelse.map { it.stønadTom }.maxOrNull() ?: LocalDate.MAX

    return TilkjentYtelse(behandlingId = behandlingId,
                          personident = søker,
                          saksbehandler = saksbehandler,
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
                                    personIdent = it.personIdent)
            }
}

fun TilkjentYtelse.tilDto(type: Stønadstype): TilkjentYtelseDTO {
    return TilkjentYtelseDTO(id = this.id,
                             behandlingId = this.behandlingId,
                             søker = this.personident,
                             andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilDto(type) })
}

fun AndelTilkjentYtelse.tilDto(type: Stønadstype): AndelTilkjentYtelseDTO {
    return AndelTilkjentYtelseDTO(beløp = this.beløp,
                                  stønadFom = this.stønadFom,
                                  stønadTom = this.stønadTom,
                                  personIdent = this.personIdent,
                                  type = type)
}

@Deprecated("Skal ikke brukes")
fun TilkjentYtelse.tilOpphør(saksbehandler: String, opphørDato: LocalDate) =
        TilkjentYtelse(type = TilkjentYtelseType.OPPHØR,
                       personident = personident,
                       saksbehandler = saksbehandler,
                       opphørFom = opphørDato,
                       behandlingId = behandlingId,
                       vedtaksdato = LocalDate.now(),
                       andelerTilkjentYtelse = andelerTilkjentYtelse)
