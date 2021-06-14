package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDto
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDto
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse

fun TilkjentYtelse.tilDto(): TilkjentYtelseDto {
    return TilkjentYtelseDto(behandlingId = this.behandlingId,
                             vedtaksdato = this.vedtaksdato,
                             andeler = this.andelerTilkjentYtelse.map { andel ->
                                 AndelTilkjentYtelseDto(beløp = andel.beløp,
                                                        stønadFra = andel.stønadFom,
                                                        stønadTil = andel.stønadTom,
                                                        inntekt = andel.inntekt,
                                                        samordningsfradrag = andel.samordningsfradrag)
                             })
}
