package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.util.Periode
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.ef.sak.util.isEqualOrBefore
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class BeregningService {

    fun beregnYtelse(beregningRequest: BeregningRequest): List<Beløpsperiode> {
        val vedtaksperioder = beregningRequest.vedtaksperiode
        val beløpForInnteksperioder = beregningRequest.inntektsperioder.map { beregnBeløpPeriode(it) }.flatten()

        return vedtaksperioder.flatMap {
            finnGjeldeneBeløpsperiode(beløpForInnteksperioder, it.fradato, it.tildato)
        }
    }

    fun finnGjeldeneBeløpsperiode(beløpForInnteksperioder: List<Beløpsperiode>, vedtaksperiodeFraOgmedDato: LocalDate, vedtaksperiodeTilDato: LocalDate): List<Beløpsperiode> {
        return beløpForInnteksperioder.mapNotNull {
            if (it.fraOgMedDato.isEqualOrAfter(vedtaksperiodeFraOgmedDato) && it.tilDato.isEqualOrBefore(vedtaksperiodeTilDato)) {
                it
            }
            else if (vedtaksperiodeTilDato.isBefore(it.tilDato) && vedtaksperiodeFraOgmedDato.isBefore(it.fraOgMedDato)) {
                it.copy(tilDato = vedtaksperiodeTilDato)
            }
            else if (vedtaksperiodeFraOgmedDato.isAfter(it.fraOgMedDato) && vedtaksperiodeFraOgmedDato.isBefore(it.fraOgMedDato)) {
                it.copy(fraOgMedDato = vedtaksperiodeFraOgmedDato)
            } else {
                null
            }
        }
    }

    fun beregnBeløpPeriode(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        return finnGrunnbeløpsPerioder(inntektsperiode.startDato, inntektsperiode.sluttDato).map {
            val samordningsfradrag = inntektsperiode.samordningsfradrag
            val inntekt = inntektsperiode.inntekt

            val avkortning = avkortningPerMåned(it.beløp, inntekt)

            val fullOvergangsStønadMåned =
                    it.beløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP)

            val utbetaling = fullOvergangsStønadMåned.subtract(avkortning).subtract(samordningsfradrag)

            Beløpsperiode(fraOgMedDato = it.fraOgMedDato,
                          tilDato = it.tilDato,
                          beløp = utbetaling,
                          beregningsgrunnlag = Beregningsgrunnlag(samordningsfradrag = samordningsfradrag,
                                                                  inntekt = inntekt,
                                                                  grunnbeløp = it.beløp))
        }
    }

    fun avkortningPerMåned(grunnbeløp: BigDecimal, inntekt: BigDecimal): BigDecimal {
        val inntektOverHalveGrunnbeløp = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
        return if (inntektOverHalveGrunnbeløp > BigDecimal(0)) inntektOverHalveGrunnbeløp.subtract(
                inntektOverHalveGrunnbeløp.multiply(
                        BigDecimal(0.45))).divide(BigDecimal(12)) else BigDecimal(0)
    }
}