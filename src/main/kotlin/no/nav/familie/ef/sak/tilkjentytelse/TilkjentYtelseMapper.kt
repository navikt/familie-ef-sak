package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.Beregningsgrunnlag
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.beregning.barnetilsyn.roundUp
import no.nav.familie.ef.sak.beregning.barnetilsyn.tilBeløpsperioderPerUtgiftsmåned
import no.nav.familie.ef.sak.beregning.tilInntekt
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun TilkjentYtelse.tilDto(): TilkjentYtelseDto =
    TilkjentYtelseDto(
        behandlingId = this.behandlingId,
        andeler = this.andelerTilkjentYtelse.map { andel -> andel.tilDto() },
        samordningsfradragType = this.samordningsfradragType,
    )

fun AndelTilkjentYtelse.tilDto(): AndelTilkjentYtelseDto =
    AndelTilkjentYtelseDto(
        beløp = this.beløp,
        periode = this.periode,
        inntekt = this.inntekt,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
    )

fun TilkjentYtelse.tilBeløpsperiode(
    vedtak: Vedtak,
): List<Beløpsperiode> {
    val startDatoForVedtak =
        vedtak.perioder
            ?.perioder
            ?.minByOrNull { it.datoFra }
            ?.datoFra
            ?: error("Fant ingen startdato for vedtak på behandling med id=$behandlingId")

    val inntekter = vedtak.inntekter?.inntekter?.tilInntekt() ?: emptyList()

    val skalBrukeMånedsinntekt =
        inntekter.isNotEmpty() &&
                inntekter.all { it.dagsats == BigDecimal.ZERO } &&
                inntekter.all { (it.forventetInntekt == BigDecimal.ZERO) }

    return this.andelerTilkjentYtelse.filter { andel -> andel.periode.fomDato >= startDatoForVedtak }.map { andel ->
        val inntekt = inntekter.sortedBy { it.årMånedFra }.lastOrNull { it.årMånedFra <= andel.periode.fom }
        val månedsinntekt = if (skalBrukeMånedsinntekt) inntekt?.månedsinntekt else null

        Beløpsperiode(
            beløp = andel.beløp.toBigDecimal(),
            periode = andel.periode,
            beregningsgrunnlag =
                Beregningsgrunnlag(
                    inntekt = andel.inntekt.toBigDecimal(),
                    samordningsfradrag = andel.samordningsfradrag.toBigDecimal(),
                    samordningsfradragType = this.samordningsfradragType,
                    avkortningPerMåned = andel.inntektsreduksjon.toBigDecimal(),
                    månedsinntekt = månedsinntekt,
                ),
            beløpFørSamordning = andel.beløp.plus(andel.samordningsfradrag).toBigDecimal(),
        )
    }
}

fun TilkjentYtelse.tilBeløpsperiodeBarnetilsyn(
    vedtak: InnvilgelseBarnetilsyn,
    brukIkkeVedtatteSatser: Boolean,
): List<BeløpsperiodeBarnetilsynDto> {
    val startDato =
        vedtak.perioder
            .first()
            .periode.fomDato
    val perioder = vedtak.tilBeløpsperioderPerUtgiftsmåned(brukIkkeVedtatteSatser)

    return this.andelerTilkjentYtelse.filter { andel -> andel.stønadFom >= startDato }.map {
        val beløpsperiodeBarnetilsynDto = perioder.getValue(YearMonth.from(it.stønadFom))
        BeløpsperiodeBarnetilsynDto(
            periode = it.periode,
            beløp = it.beløp,
            beløpFørFratrekkOgSatsjustering =
                BeregningBarnetilsynUtil
                    .kalkulerUtbetalingsbeløpFørFratrekkOgSatsjustering(
                        beløpsperiodeBarnetilsynDto.beregningsgrunnlag.utgifter,
                        beløpsperiodeBarnetilsynDto.beregningsgrunnlag.kontantstøttebeløp,
                    ).roundUp()
                    .toInt(),
            sats = beløpsperiodeBarnetilsynDto.sats,
            beregningsgrunnlag = beløpsperiodeBarnetilsynDto.beregningsgrunnlag,
            aktivitetstype = beløpsperiodeBarnetilsynDto.aktivitetstype,
            periodetype = beløpsperiodeBarnetilsynDto.periodetype,
        )
    }
}

fun TilkjentYtelse.tilTilkjentYtelseMedMetaData(
    saksbehandlerId: String,
    eksternBehandlingId: Long,
    stønadstype: StønadType,
    eksternFagsakId: Long,
    vedtaksdato: LocalDate,
): TilkjentYtelseMedMetadata =
    TilkjentYtelseMedMetadata(
        tilkjentYtelse = this.tilIverksettDto(),
        saksbehandlerId = saksbehandlerId,
        eksternBehandlingId = eksternBehandlingId,
        stønadstype = stønadstype,
        eksternFagsakId = eksternFagsakId,
        personIdent = this.personident,
        behandlingId = this.behandlingId,
        vedtaksdato = vedtaksdato,
    )
