package no.nav.familie.ef.sak.api.beregning

import org.springframework.stereotype.Service
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class BeregningService {

    fun beregnYtelse(beregningRequest: BeregningRequest): List<Beløpsperiode> {

        return beregningRequest.inntektsPerioder.map { beregnBeløpPeriode(it) }.flatten()
    }

    fun beregnBeløpPeriode(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        return finnGrunnbeløpsPerioder(inntektsperiode.startDato, inntektsperiode.sluttDato).map {
            val avkortning = avkort(it.beløp, inntektsperiode.inntekt)
            val fullOvergangsStønadMåned =
                    it.beløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP)
            val utbetaling = fullOvergangsStønadMåned.subtract(avkortning.divide(BigDecimal(12)))
                    .subtract(inntektsperiode.samordningFradrag)

            Beløpsperiode(it.fraOgMedDato, it.tilDato, utbetaling)
        }
    }

    fun avkort(grunnbeløp: BigDecimal, inntekt: BigDecimal): BigDecimal {
        val inntektOverHalveGrunnbeløp = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
        return if (inntektOverHalveGrunnbeløp > BigDecimal(0)) inntektOverHalveGrunnbeløp.subtract(
                inntektOverHalveGrunnbeløp.multiply(
                        BigDecimal(0.45))) else BigDecimal(0)
    }
}