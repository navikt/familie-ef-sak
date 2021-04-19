package no.nav.familie.ef.sak.api.beregning

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class BeregningService {

    fun beregnYtelse(beregningRequest: BeregningRequest): List<Beløpsperiode> {

        return beregningRequest.inntektsPerioder.map { inntektsperiode ->
            return finnGrunnbeløpsPerioder(inntektsperiode.startDato, inntektsperiode.sluttDato).map {
                val inntektOverHalveGrunnbeløp = inntektsperiode.inntekt.subtract(it.beløp.multiply(BigDecimal(0.5)))
                val avkortning =
                        if (inntektOverHalveGrunnbeløp <= BigDecimal(0)) inntektOverHalveGrunnbeløp.subtract(
                                inntektOverHalveGrunnbeløp.multiply(
                                        BigDecimal(0.45))) else BigDecimal(0)
                val fullOvergangsStønadMåned =
                        it.beløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP)
                val utbetaling = fullOvergangsStønadMåned - avkortning

                Beløpsperiode(it.fraOgMedDato, it.tilDato, utbetaling)
            }
        }
    }
}