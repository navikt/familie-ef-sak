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


            val beløpTilUtbetalning = if(utbetaling <= BigDecimal.ZERO) BigDecimal.ZERO else utbetaling

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
        return if (inntektOverHalveGrunnbeløp > BigDecimal(0))
            inntektOverHalveGrunnbeløp.multiply(BigDecimal(0.45)).setScale(5, RoundingMode.HALF_DOWN) else BigDecimal(0)
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


    private fun validerVedtaksperioder(vedtaksperiode: List<Periode>) {
        val sorterteVedtaksperioder = vedtaksperiode.sortedBy { it.fradato }

        sorterteVedtaksperioder.forEachIndexed { index, periode ->
            if (sorterteVedtaksperioder.size > 1 && index < sorterteVedtaksperioder.lastIndex) {
                feilHvis(periode.tildato.isEqualOrAfter(sorterteVedtaksperioder[index + 1].fradato)) {
                    "Vedtaksperioder ${vedtaksperiode} overlapper"
                }
            }
        }
    }

    private fun validerInnteksperioder(inntektsperioder: List<Inntektsperiode>, vedtaksperiode: List<Periode>) {
        feilHvis(inntektsperioder.isEmpty()) {
            "Inntektsperioder kan ikke være tom liste"
        }

        val sorterteIntekter = inntektsperioder.sortedBy { it.startDato }
        val sorterteVedtaksperioder = vedtaksperiode.sortedBy { it.fradato }


        sorterteIntekter.forEachIndexed { index, inntektsperiode ->
            if (index === 0) {
                feilHvis(!inntektsperiode.startDato.isEqualOrBefore(sorterteVedtaksperioder.first().fradato)) {
                    "Inntektsperioder ${inntektsperioder} begynner etter vedtaksperioder ${vedtaksperiode}"
                }
            }
            if (index === sorterteIntekter.lastIndex) {
                feilHvis(!inntektsperiode.sluttDato.isEqualOrAfter(sorterteVedtaksperioder.last().tildato)) {
                    "Inntektsperioder ${inntektsperioder} slutter før vedtaksperioder ${vedtaksperiode} "
                }
            }
            if (sorterteIntekter.size > 1 && index < sorterteIntekter.lastIndex) {
                feilHvis(inntektsperiode.startDato.isEqual(sorterteIntekter[index + 1].startDato)) {
                    "Inntektsperioder ${inntektsperioder} overlapper eller er ikke sammenhengde for vedtaksperioder ${vedtaksperiode}"
                }
            }

            feilHvis(inntektsperiode.inntekt < BigDecimal.ZERO) {
                "Inntekten kan ikke vara mindre en null:  ${inntektsperiode.inntekt}"
            }

            feilHvis(inntektsperiode.samordningsfradrag < BigDecimal.ZERO) {
                "Samordningsfradraget kan ikke vara mindre en null: ${inntektsperiode.samordningsfradrag}"
            }
        }
    }
}