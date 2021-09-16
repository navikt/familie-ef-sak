package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.util.Periode
import java.math.BigDecimal
import java.math.RoundingMode

class BeregningUtils {
    companion object {

        val REDUKSJONSFAKTOR = BigDecimal(0.45)

        fun beregnStønadForInntekt(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
            val (startDato, sluttDato, inntekt, samordningsfradrag) = inntektsperiode
            return finnGrunnbeløpsPerioder(startDato, sluttDato).map {
                val avkortningPerMåned = beregnAvkortning(it.beløp, inntekt).divide(BigDecimal(12))
                        .setScale(0, RoundingMode.HALF_DOWN)

                val fullOvergangsStønadPerMåned =
                        it.beløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_EVEN)

                val beløpFørSamordning =
                        fullOvergangsStønadPerMåned.subtract(avkortningPerMåned).setScale(0, RoundingMode.HALF_UP)

                val utbetaling = beløpFørSamordning.subtract(samordningsfradrag).setScale(0, RoundingMode.HALF_UP)


                val beløpTilUtbetalning = if (utbetaling <= BigDecimal.ZERO) BigDecimal.ZERO else utbetaling

                Beløpsperiode(periode = it.periode,
                              beløp = beløpTilUtbetalning,
                              beløpFørSamordning = beløpFørSamordning,
                              beregningsgrunnlag = Beregningsgrunnlag(samordningsfradrag = samordningsfradrag,
                                                                      avkortningPerMåned = avkortningPerMåned,
                                                                      fullOvergangsStønadPerMåned = fullOvergangsStønadPerMåned,
                                                                      inntekt = inntekt,
                                                                      grunnbeløp = it.beløp))
            }
        }


        fun beregnAvkortning(grunnbeløp: BigDecimal, inntekt: BigDecimal): BigDecimal {
            val inntektOverHalveGrunnbeløp = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
            return if (inntektOverHalveGrunnbeløp > BigDecimal.ZERO)
                inntektOverHalveGrunnbeløp.multiply(REDUKSJONSFAKTOR).setScale(5, RoundingMode.HALF_DOWN) else BigDecimal.ZERO
        }

        fun finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder: List<Beløpsperiode>,
                                                             vedtaksperiode: Periode): List<Beløpsperiode> {
            return beløpForInnteksperioder.mapNotNull {
                when {
                    it.periode.omsluttesAv(vedtaksperiode) -> {
                        it
                    }
                    it.periode.overlapperIStartenAv(vedtaksperiode) -> {
                        it.copy(periode = it.periode.copy(fradato = vedtaksperiode.fradato))
                    }
                    vedtaksperiode.overlapperIStartenAv(it.periode) -> {
                        it.copy(periode = it.periode.copy(tildato = vedtaksperiode.tildato))
                    }
                    vedtaksperiode.omsluttesAv(it.periode) -> {
                        it.copy(periode = it.periode.copy(fradato = vedtaksperiode.fradato, tildato = vedtaksperiode.tildato))
                    }
                    else -> {
                        null
                    }
                }
            }
        }

    }
}