package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.api.feilHvis
import no.nav.familie.ef.sak.util.Periode
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.ef.sak.util.isEqualOrBefore
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class BeregningService {

    private val REDUKSJONSFAKTOR = BigDecimal(0.45)

    fun beregnYtelse(vedtaksperioder: List<Periode>, inntektsperioder: List<Inntektsperiode>): List<Beløpsperiode> {

        validerInnteksperioder(inntektsperioder, vedtaksperioder)
        validerVedtaksperioder(vedtaksperioder)


        val beløpForInnteksperioder = inntektsperioder.flatMap { beregnBeløpForInntekt(it) }

        return vedtaksperioder.flatMap {
            finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder, it.fradato, it.tildato)
        }
    }


    private fun beregnBeløpForInntekt(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
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

            Beløpsperiode(fraOgMedDato = it.fraOgMedDato,
                          tilDato = it.tilDato,
                          beløp = beløpTilUtbetalning,
                          beløpFørSamordning = beløpFørSamordning,
                          beregningsgrunnlag = Beregningsgrunnlag(samordningsfradrag = samordningsfradrag,
                                                                  avkortningPerMåned = avkortningPerMåned,
                                                                  fullOvergangsStønadPerMåned = fullOvergangsStønadPerMåned,
                                                                  inntekt = inntekt,
                                                                  grunnbeløp = it.beløp))
        }
    }


    private fun beregnAvkortning(grunnbeløp: BigDecimal, inntekt: BigDecimal): BigDecimal {
        val inntektOverHalveGrunnbeløp = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
        return if (inntektOverHalveGrunnbeløp > BigDecimal.ZERO)
            inntektOverHalveGrunnbeløp.multiply(REDUKSJONSFAKTOR).setScale(5, RoundingMode.HALF_DOWN) else BigDecimal.ZERO
    }


    private fun finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder: List<Beløpsperiode>,
                                                         vedtaksperiodeFraOgmedDato: LocalDate,
                                                         vedtaksperiodeTilDato: LocalDate): List<Beløpsperiode> {
        return beløpForInnteksperioder.mapNotNull {
            if (it.fraOgMedDato.isEqualOrAfter(vedtaksperiodeFraOgmedDato) && it.tilDato.isEqualOrBefore(vedtaksperiodeTilDato)) {
                it
            } else if (it.starterFørVedtaksperiodeOgOverlapper(vedtaksperiodeFraOgmedDato, vedtaksperiodeTilDato)) {
                it.copy(fraOgMedDato = vedtaksperiodeFraOgmedDato)
            } else if (it.starterEtterVedtaksperiodeOgOverlapper(
                            vedtaksperiodeFraOgmedDato,
                            vedtaksperiodeTilDato)) {
                it.copy(tilDato = vedtaksperiodeTilDato)
            } else if (it.starterFørOgSlutterEtterVedtaksperiode(vedtaksperiodeFraOgmedDato,
                                                                 vedtaksperiodeTilDato)) {
                it.copy(tilDato = vedtaksperiodeTilDato, fraOgMedDato = vedtaksperiodeFraOgmedDato)
            } else {
                null
            }
        }
    }


    private fun validerVedtaksperioder(vedtaksperioder: List<Periode>) {
        feilHvis(vedtaksperioder.zipWithNext { a, b -> a.tildato.isEqualOrAfter(b.fradato) }
                         .any { it }) { "Vedtaksperioder ${vedtaksperioder} overlapper" }
    }

    private fun validerInnteksperioder(inntektsperioder: List<Inntektsperiode>, vedtaksperioder: List<Periode>) {
        feilHvis(inntektsperioder.isEmpty()) {
            "Inntektsperioder kan ikke være tom liste"
        }

        feilHvis(inntektsperioder.zipWithNext { a, b -> a.startDato.isBefore(b.startDato) && a.sluttDato.isBefore(b.sluttDato) }
                         .any { !it }) { "Inntektsperioder må være sortert" }
        feilHvis(vedtaksperioder.zipWithNext { a, b -> a.fradato.isBefore(b.fradato) && a.tildato.isBefore(b.tildato) }
                         .any { !it }) { "Vedtaksperioder må være sortert" }

        feilHvis(!inntektsperioder.first().startDato.isEqualOrBefore(vedtaksperioder.first().fradato)) {
            "Inntektsperioder $inntektsperioder begynner etter vedtaksperioder $vedtaksperioder"
        }

        feilHvis(!inntektsperioder.last().sluttDato.isEqualOrAfter(vedtaksperioder.last().tildato)) {
            "Inntektsperioder $inntektsperioder slutter før vedtaksperioder $vedtaksperioder "
        }

        feilHvis(inntektsperioder.any { it.inntekt < BigDecimal.ZERO }) { "Inntekten kan ikke være negativt" }
        feilHvis(inntektsperioder.any { it.samordningsfradrag < BigDecimal.ZERO }) { "Samordningsfradraget kan ikke være negativt" }

        feilHvis(inntektsperioder.zipWithNext { a, b -> a.sluttDato.isEqual(b.startDato.minusDays(1)) }
                         .any { !it }) { "Inntektsperioder ${inntektsperioder} overlapper eller er ikke sammenhengde" }
    }
}