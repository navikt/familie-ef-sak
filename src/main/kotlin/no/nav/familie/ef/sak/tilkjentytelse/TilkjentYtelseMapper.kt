package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.Beregningsgrunnlag
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.split
import no.nav.familie.ef.sak.beregning.barnetilsyn.tilBeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDate
import java.time.YearMonth

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

fun TilkjentYtelse.tilBeløpsperiodeBarnetilsyn(vedtak: InnvilgelseBarnetilsyn): List<BeløpsperiodeBarnetilsynDto> {
    val startDato = vedtak.perioder.first().årMånedFra.atDay(1)
    val perioder = vedtak.perioder.map { it.split() }.flatten().associate {
        it.årMåned to Pair(it,
                           it.tilBeløpsperiodeBarnetilsynDto(vedtak.perioderKontantstøtte,
                                                             vedtak.tilleggsstønad.perioder))
    }

    return this.andelerTilkjentYtelse.filter { andel -> andel.stønadFom >= startDato }.map {
        val pair = perioder.getValue(YearMonth.from(it.stønadFom))
        BeløpsperiodeBarnetilsynDto(Periode(it.stønadFom, it.stønadTom), it.beløp,
                                    BeregningsgrunnlagBarnetilsynDto(pair.first.utgifter,
                                                                     pair.second.beregningsgrunnlag.kontantstøttebeløp,
                                                                     pair.second.beregningsgrunnlag.tilleggsstønadsbeløp,
                                                                     pair.first.barn.size,
                                                                     pair.first.barn))
    }
}

fun TilkjentYtelse.tilTilkjentYtelseMedMetaData(saksbehandlerId: String,
                                                eksternBehandlingId: Long,
                                                stønadstype: StønadType,
                                                eksternFagsakId: Long): TilkjentYtelseMedMetadata {
    return TilkjentYtelseMedMetadata(tilkjentYtelse = this.tilIverksettDto(),
                                     saksbehandlerId = saksbehandlerId,
                                     eksternBehandlingId = eksternBehandlingId,
                                     stønadstype = stønadstype,
                                     eksternFagsakId = eksternFagsakId,
                                     personIdent = this.personident,
                                     behandlingId = this.behandlingId,
                                     vedtaksdato = this.vedtakstidspunkt.toLocalDate())
}

