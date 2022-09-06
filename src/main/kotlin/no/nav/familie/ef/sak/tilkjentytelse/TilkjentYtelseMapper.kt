package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.Beregningsgrunnlag
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.beregning.barnetilsyn.roundUp
import no.nav.familie.ef.sak.beregning.barnetilsyn.tilBeløpsperioderPerUtgiftsmåned
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDate

fun TilkjentYtelse.tilDto(): TilkjentYtelseDto {
    return TilkjentYtelseDto(
        behandlingId = this.behandlingId,
        vedtakstidspunkt = this.vedtakstidspunkt,
        andeler = this.andelerTilkjentYtelse.map { andel -> andel.tilDto() },
        samordningsfradragType = this.samordningsfradragType
    )
}

fun AndelTilkjentYtelse.tilDto(): AndelTilkjentYtelseDto {
    return AndelTilkjentYtelseDto(
        beløp = this.beløp,
        periode = this.periode.toMånedsperiode(),
        inntekt = this.inntekt,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag
    )
}

fun TilkjentYtelse.tilBeløpsperiode(startDato: LocalDate): List<Beløpsperiode> {
    return this.andelerTilkjentYtelse.filter { andel -> andel.periode.fom >= startDato }.map { andel ->
        Beløpsperiode(
            beløp = andel.beløp.toBigDecimal(),
            periode = andel.periode.toMånedsperiode(),
            beregningsgrunnlag = Beregningsgrunnlag(
                inntekt = andel.inntekt.toBigDecimal(),
                samordningsfradrag = andel.samordningsfradrag.toBigDecimal(),
                samordningsfradragType = this.samordningsfradragType,
                avkortningPerMåned = andel.inntektsreduksjon.toBigDecimal()
            ),
            beløpFørSamordning = andel.beløp.plus(andel.samordningsfradrag).toBigDecimal()
        )
    }
}

fun TilkjentYtelse.tilBeløpsperiodeBarnetilsyn(vedtak: InnvilgelseBarnetilsyn): List<BeløpsperiodeBarnetilsynDto> {
    val startDato = vedtak.perioder.first().periode.fomDato
    val perioder = vedtak.tilBeløpsperioderPerUtgiftsmåned()

    return this.andelerTilkjentYtelse.filter { andel -> andel.periode.fom >= startDato }.map {
        val beløpsperiodeBarnetilsynDto = perioder.getValue(it.periode.fomMåned)
        BeløpsperiodeBarnetilsynDto(
            periode = it.periode.toMånedsperiode(),
            beløp = it.beløp,
            beløpFørFratrekkOgSatsjustering = BeregningBarnetilsynUtil.kalkulerUtbetalingsbeløpFørFratrekkOgSatsjustering(
                beløpsperiodeBarnetilsynDto.beregningsgrunnlag.utgifter,
                beløpsperiodeBarnetilsynDto.beregningsgrunnlag.kontantstøttebeløp
            )
                .roundUp()
                .toInt(),
            sats = beløpsperiodeBarnetilsynDto.sats,
            beregningsgrunnlag = beløpsperiodeBarnetilsynDto.beregningsgrunnlag
        )
    }
}

fun TilkjentYtelse.tilTilkjentYtelseMedMetaData(
    saksbehandlerId: String,
    eksternBehandlingId: Long,
    stønadstype: StønadType,
    eksternFagsakId: Long
): TilkjentYtelseMedMetadata {
    return TilkjentYtelseMedMetadata(
        tilkjentYtelse = this.tilIverksettDto(),
        saksbehandlerId = saksbehandlerId,
        eksternBehandlingId = eksternBehandlingId,
        stønadstype = stønadstype,
        eksternFagsakId = eksternFagsakId,
        personIdent = this.personident,
        behandlingId = this.behandlingId,
        vedtaksdato = this.vedtakstidspunkt.toLocalDate()
    )
}
