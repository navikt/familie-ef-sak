package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.api.feilHvis
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.ef.sak.util.isEqualOrBefore
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class BeregningService {

    fun beregnYtelse(beregningRequest: BeregningRequest): List<Beløpsperiode> {

        val sorterteIntekter = beregningRequest.inntektsperioder.sortedBy { it.startDato }
        val sorterteVedtaksperioder = beregningRequest.vedtaksperiode.sortedBy { it.fradato }

        sorterteIntekter.forEachIndexed { index, inntektsperiode ->
            if(index === sorterteIntekter.lastIndex){
                feilHvis(!inntektsperiode.sluttDato.isEqual(sorterteVedtaksperioder.last().tildato)){
                    "Inntektsperioder ${beregningRequest.inntektsperioder} dekker ikke alle vedtaksperioder "
                }
            }

            inntektsperiode.sluttDato.isEqual(sorterteIntekter.get(index+1))
        }


        val vedtaksperioder = beregningRequest.vedtaksperiode
        val beløpForInnteksperioder = beregningRequest.inntektsperioder.map { beregnBeløpPeriode(it) }.flatten()

        return vedtaksperioder.flatMap {
            finnGjeldeneBeløpsperiode(beløpForInnteksperioder, it.fradato, it.tildato)
        }
    }

    fun finnGjeldeneBeløpsperiode(beløpForInnteksperioder: List<Beløpsperiode>,
                                  vedtaksperiodeFraOgmedDato: LocalDate,
                                  vedtaksperiodeTilDato: LocalDate): List<Beløpsperiode> {
        val v = beløpForInnteksperioder.mapNotNull {
            if (it.fraOgMedDato.isEqualOrAfter(vedtaksperiodeFraOgmedDato) && it.tilDato.isEqualOrBefore(vedtaksperiodeTilDato)) {
                it
            } else if (beløpsperiodeStarterFørVedtaksperiodeOgOverlapper(it, vedtaksperiodeFraOgmedDato, vedtaksperiodeTilDato)) {
                it.copy(fraOgMedDato = vedtaksperiodeFraOgmedDato)
            } else if (beløpsperiodeStarterEtterVedtaksperiodeOgOverlapper(it,
                                                                           vedtaksperiodeFraOgmedDato,
                                                                           vedtaksperiodeTilDato)) {
                it.copy(tilDato = vedtaksperiodeTilDato)
            } else if (it.fraOgMedDato.isBefore(vedtaksperiodeFraOgmedDato) && it.tilDato.isAfter(vedtaksperiodeTilDato)) {
                it.copy(tilDato = vedtaksperiodeTilDato, fraOgMedDato = vedtaksperiodeFraOgmedDato)
            } else {
                null
            }
        }
        return v
    }
    /*

     */

    private fun beløpsperiodeStarterEtterVedtaksperiodeOgOverlapper(it: Beløpsperiode,
                                                                    vedtaksperiodeFraOgmedDato: LocalDate,
                                                                    vedtaksperiodeTilDato: LocalDate) =
            it.fraOgMedDato.isAfter(vedtaksperiodeFraOgmedDato) && it.fraOgMedDato.isBefore(vedtaksperiodeTilDato)

    private fun beløpsperiodeStarterFørVedtaksperiodeOgOverlapper(it: Beløpsperiode,
                                                                  vedtaksperiodeFraOgmedDato: LocalDate,
                                                                  vedtaksperiodeTilDato: LocalDate) =
            it.tilDato.isBefore(vedtaksperiodeTilDato) && it.tilDato.isAfter(vedtaksperiodeFraOgmedDato)

    fun beregnBeløpPeriode(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        return finnGrunnbeløpsPerioder(inntektsperiode.startDato, inntektsperiode.sluttDato).map {
            val samordningsfradrag = inntektsperiode.samordningsfradrag
            val inntekt = inntektsperiode.inntekt

            val avkortning = avkortningPerMåned(it.beløp, inntekt)
            val fullOvergangsStønad =
                    it.beløp.multiply(BigDecimal(2.25))

            val utbetaling = fullOvergangsStønad.subtract(avkortning)
                    .subtract(samordningsfradrag)
                    .divide(BigDecimal(12))
                    .setScale(0, RoundingMode.HALF_UP)

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
        return if (inntektOverHalveGrunnbeløp > BigDecimal(0))
            inntektOverHalveGrunnbeløp.multiply(BigDecimal(0.45)).setScale(5, RoundingMode.HALF_DOWN) else BigDecimal(0)
    }
}