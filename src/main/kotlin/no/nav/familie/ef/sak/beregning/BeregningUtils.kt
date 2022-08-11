package no.nav.familie.ef.sak.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

object BeregningUtils {

    private val REDUKSJONSFAKTOR = BigDecimal(0.45)

    fun beregnStønadForInntekt(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        val (_, _, inntekt, samordningsfradrag) = inntektsperiode
        val periode = inntektsperiode.periode
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
                deprecatedPeriode = it.deprecatedPeriode,
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
        return if (inntektOverHalveGrunnbeløp > BigDecimal.ZERO)
            inntektOverHalveGrunnbeløp.multiply(REDUKSJONSFAKTOR).setScale(5, RoundingMode.HALF_DOWN) else BigDecimal.ZERO
    }

    fun indeksjusterInntekt(
        sisteBrukteGrunnbeløpsdato: LocalDate,
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
        val (_, _, inntekt, samordningsfradrag) = inntektsperiode
        val periode = inntektsperiode.periode
        return finnGrunnbeløpsPerioder(periode).map { grunnbeløp ->
            if (grunnbeløp.periode.fom > sistBrukteGrunnbeløp.periode.fom &&
                grunnbeløp.beløp != sistBrukteGrunnbeløp.grunnbeløp
            ) {
                val faktor = grunnbeløp.beløp.divide(sistBrukteGrunnbeløp.grunnbeløp, MathContext.DECIMAL128)
                val justerInntekt = inntekt.multiply(faktor).setScale(0, RoundingMode.FLOOR).toLong()
                val justerInntektAvrundetNedTilNærmeste100 = (justerInntekt / 100L) * 100L
                Inntektsperiode(
                    grunnbeløp.periode.toMånedsperiode(),
                    BigDecimal(justerInntektAvrundetNedTilNærmeste100),
                    samordningsfradrag
                )
            } else {
                Inntektsperiode(grunnbeløp.periode.toMånedsperiode(), inntekt, samordningsfradrag)
            }
        }
    }

    fun finnStartDatoOgSluttDatoForBeløpsperiode(
        beløpForInnteksperioder: List<Beløpsperiode>,
        vedtaksperiode: Månedsperiode
    ): List<Beløpsperiode> {
        val vedtaksdatoperiode = vedtaksperiode.toDatoperiode()
        return beløpForInnteksperioder.mapNotNull {
            when {
                it.periode.omsluttesAv(vedtaksdatoperiode) -> {
                    it
                }
                it.periode.overlapperKunIStartenAv(vedtaksdatoperiode) -> {
                    it.copy(
                        deprecatedPeriode = it.deprecatedPeriode.copy(fradato = vedtaksdatoperiode.fom),
                        periode = (it.periode snitt vedtaksdatoperiode)!!
                    )
                }
                vedtaksdatoperiode.overlapperKunIStartenAv(it.periode) -> {
                    it.copy(
                        deprecatedPeriode = it.deprecatedPeriode.copy(tildato = vedtaksdatoperiode.tom),
                        periode = (it.periode snitt vedtaksdatoperiode)!!
                    )
                }
                vedtaksdatoperiode.omsluttesAv(it.periode) -> {
                    it.copy(
                        deprecatedPeriode = it.deprecatedPeriode.copy(fradato = vedtaksdatoperiode.fom, tildato = vedtaksdatoperiode.tom),
                        periode = (it.periode snitt vedtaksdatoperiode)!!
                    )
                }
                else -> {
                    null
                }
            }
        }
    }
}
