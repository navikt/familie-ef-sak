package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.Beregningsgrunnlag
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import java.time.LocalDate

fun TilkjentYtelse.tilDto(): TilkjentYtelseDto {
    return TilkjentYtelseDto(behandlingId = this.behandlingId,
                             vedtakstidspunkt = this.vedtakstidspunkt,
                             andeler = this.andelerTilkjentYtelse.map { andel -> andel.tilDto() },
                             samordningsfradragType = this.samordningsfradragType)
}

fun AndelTilkjentYtelse.tilDto(): AndelTilkjentYtelseDto {
    return AndelTilkjentYtelseDto(beløp = this.beløp,
                                  stønadFra = this.stønadFom,
                                  stønadTil = this.stønadTom,
                                  inntekt = this.inntekt,
                                  inntektsreduksjon = this.inntektsreduksjon,
                                  samordningsfradrag = this.samordningsfradrag)

}

fun TilkjentYtelse.tilBeløpsperiode(startDato: LocalDate): List<Beløpsperiode> {
    return this.andelerTilkjentYtelse.filter { andel -> andel.stønadFom >= startDato }.map { andel ->
        Beløpsperiode(beløp = andel.beløp.toBigDecimal(),
                      periode = Periode(fradato = andel.stønadFom, tildato = andel.stønadTom),
                      beregningsgrunnlag = Beregningsgrunnlag(inntekt = andel.inntekt.toBigDecimal(),
                                                              samordningsfradrag = andel.samordningsfradrag.toBigDecimal(),
                                                              samordningsfradragType = this.samordningsfradragType,
                                                              avkortningPerMåned = andel.inntektsreduksjon.toBigDecimal()),
                      beløpFørSamordning = andel.beløp.plus(andel.samordningsfradrag).toBigDecimal())
    }
}

fun TilkjentYtelse.tilTilkjentYtelseMedMetaData(saksbehandlerId: String,
                                                eksternBehandlingId: Long,
                                                stønadstype: Stønadstype,
                                                eksternFagsakId: Long): TilkjentYtelseMedMetadata {
    return TilkjentYtelseMedMetadata(tilkjentYtelse = this.tilIverksettDto(),
                                     saksbehandlerId = saksbehandlerId,
                                     eksternBehandlingId = eksternBehandlingId,
                                     stønadstype = StønadType.valueOf(stønadstype.name),
                                     eksternFagsakId = eksternFagsakId,
                                     personIdent = this.personident,
                                     behandlingId = this.behandlingId,
                                     vedtaksdato = this.vedtakstidspunkt.toLocalDate())
}

