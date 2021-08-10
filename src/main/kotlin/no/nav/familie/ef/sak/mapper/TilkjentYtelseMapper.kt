package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.beregning.Beløpsperiode
import no.nav.familie.ef.sak.api.beregning.Beregningsgrunnlag
import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDto
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDto
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.util.Periode

fun TilkjentYtelse.tilDto(): TilkjentYtelseDto {
    return TilkjentYtelseDto(behandlingId = this.behandlingId,
                             vedtakstidspunkt = this.vedtakstidspunkt,
                             andeler = this.andelerTilkjentYtelse.map { andel ->
                                 AndelTilkjentYtelseDto(beløp = andel.beløp,
                                                        stønadFra = andel.stønadFom,
                                                        stønadTil = andel.stønadTom,
                                                        inntekt = andel.inntekt,
                                                        samordningsfradrag = andel.samordningsfradrag)
                             })
}

fun TilkjentYtelse.tilBeløpsperiode(): List<Beløpsperiode> {
    return this.andelerTilkjentYtelse.map { andel ->
        Beløpsperiode(beløp = andel.beløp.toBigDecimal(),
                      periode = Periode(fradato = andel.stønadFom, tildato = andel.stønadTom),
                      beregningsgrunnlag = Beregningsgrunnlag(inntekt = andel.inntekt.toBigDecimal(),
                                                              samordningsfradrag = andel.samordningsfradrag.toBigDecimal(),
                                                              avkortningPerMåned = andel.inntektsreduksjon.toBigDecimal()),
                      beløpFørSamordning = andel.beløp.plus(andel.samordningsfradrag).toBigDecimal())
    }
}
