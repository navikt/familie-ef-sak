package no.nav.familie.ef.sak.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.YearMonth

object BeregningUtils {

    private val REDUKSJONSFAKTOR = BigDecimal(0.45)

    fun beregnStønadForInntekt(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        val (periode, inntekt, samordningsfradrag) = inntektsperiode
        return finnGrunnbeløpsPerioder(periode).map {
            val avkortningPerMåned = beregnAvkortning(it.beløp, inntekt).divide(BigDecimal(12))
                .setScale(0, RoundingMode.HALF_DOWN)

            val fullOvergangsStønadPerMåned =
                it.beløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_EVEN)

            val beløpFørSamordningUtenAvrunding =
                fullOvergangsStønadPerMåned.subtract(avkortningPerMåned).setScale(0, RoundingMode.HALF_UP)

            val utbetaling = beløpFørSamordningUtenAvrunding.subtract(samordningsfradrag).setScale(0, RoundingMode.HALF_UP)

            val beløpFørSamordning =
                if (beløpFørSamordningUtenAvrunding <= BigDecimal.ZERO) BigDecimal.ZERO else beløpFørSamordningUtenAvrunding

            val beløpTilUtbetalning = if (utbetaling <= BigDecimal.ZERO) BigDecimal.ZERO else utbetaling

            Beløpsperiode(
                periode = it.periode,
                beløp = beløpTilUtbetalning,
                beløpFørSamordning = beløpFørSamordning,
                beregningsgrunnlag = Beregningsgrunnlag(
                    samordningsfradrag = samordningsfradrag,
                    avkortningPerMåned = avkortningPerMåned,
                    fullOvergangsStønadPerMåned = fullOvergangsStønadPerMåned,
                    inntekt = inntekt,
                    grunnbeløp = it.beløp
                )
            )
        }
    }

    private fun beregnAvkortning(grunnbeløp: BigDecimal, inntekt: BigDecimal): BigDecimal {
        val inntektOverHalveGrunnbeløp = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
        return if (inntektOverHalveGrunnbeløp > BigDecimal.ZERO) {
            inntektOverHalveGrunnbeløp.multiply(REDUKSJONSFAKTOR).setScale(5, RoundingMode.HALF_DOWN)
        } else {
            BigDecimal.ZERO
        }
    }

    fun indeksjusterInntekt(
        sisteBrukteGrunnbeløpsdato: YearMonth,
        inntekter: List<Inntektsperiode> = emptyList()
    ): List<Inntektsperiode> {
        val sistBrukteGrunnbeløp = finnGrunnbeløp(sisteBrukteGrunnbeløpsdato)
        if (nyesteGrunnbeløp == sistBrukteGrunnbeløp) {
            return inntekter
        }

        return inntekter.flatMap { justerInntektsperiode(it, sistBrukteGrunnbeløp) }
    }

    private fun justerInntektsperiode(
        inntektsperiode: Inntektsperiode,
        sistBrukteGrunnbeløp: Grunnbeløp
    ): List<Inntektsperiode> {
        val (periode, inntekt, samordningsfradrag) = inntektsperiode
        return finnGrunnbeløpsPerioder(periode).map { grunnbeløp ->
            if (grunnbeløp.periode.fom > sistBrukteGrunnbeløp.periode.fom &&
                grunnbeløp.beløp != sistBrukteGrunnbeløp.grunnbeløp
            ) {
                val faktor = grunnbeløp.beløp.divide(sistBrukteGrunnbeløp.grunnbeløp, MathContext.DECIMAL128)
                val justerInntekt = inntekt.multiply(faktor).setScale(0, RoundingMode.FLOOR).toLong()
                val justerInntektAvrundetNedTilNærmeste100 = (justerInntekt / 100L) * 100L
                Inntektsperiode(
                    grunnbeløp.periode,
                    BigDecimal(justerInntektAvrundetNedTilNærmeste100),
                    samordningsfradrag
                )
            } else {
                Inntektsperiode(grunnbeløp.periode, inntekt, samordningsfradrag)
            }
        }
    }

    fun finnStartDatoOgSluttDatoForBeløpsperiode(
        beløpForInnteksperioder: List<Beløpsperiode>,
        vedtaksperiode: Månedsperiode
    ): List<Beløpsperiode> {
        return beløpForInnteksperioder.mapNotNull {
            when {
                it.periode.omsluttesAv(vedtaksperiode) -> {
                    it
                }
                it.periode.overlapperKunIStartenAv(vedtaksperiode) -> {
                    it.copy(periode = (it.periode snitt vedtaksperiode)!!)
                }
                vedtaksperiode.overlapperKunIStartenAv(it.periode) -> {
                    it.copy(periode = (it.periode snitt vedtaksperiode)!!)
                }
                vedtaksperiode.omsluttesAv(it.periode) -> {
                    it.copy(periode = (it.periode snitt vedtaksperiode)!!)
                }
                else -> {
                    null
                }
            }
        }
    }
}
