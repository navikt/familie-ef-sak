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

    fun beregnYtelse(beregningRequest: BeregningRequest): List<Beløpsperiode> {
        val vedtaksperioder = beregningRequest.vedtaksperiode
        val vedtaksperiodeTilDato = beregningRequest.vedtaksperiode.maxOf { it.tildato }

        val mapTilInntektMedPeriode: List<Inntektsperiode> = beregningRequest.inntektsperioder
                .mapIndexed { index, inntektsperiode ->
                    if (index < beregningRequest.inntektsperioder.lastIndex && beregningRequest.inntektsperioder.size > 1) {
                        Inntektsperiode(inntekt = inntektsperiode.inntekt,
                                        samordningsfradrag = inntektsperiode.samordningsfradrag,
                                        startDato = inntektsperiode.startDato,
                                        sluttDato = beregningRequest.inntektsperioder[index + 1].startDato.minusDays(
                                               1))
                    } else {
                        Inntektsperiode(inntekt = inntektsperiode.inntekt,
                                        samordningsfradrag = inntektsperiode.samordningsfradrag,
                                        startDato = inntektsperiode.startDato,
                                        sluttDato = vedtaksperiodeTilDato)

                    }

                }

        validerInnteksperioder(mapTilInntektMedPeriode, vedtaksperioder)

        validerVedtaksperioder(vedtaksperioder)


        val beløpForInnteksperioder = mapTilInntektMedPeriode.flatMap { beregnBeløpPeriode(it) }

        return vedtaksperioder.flatMap {
            finnGjeldeneBeløpsperiode(beløpForInnteksperioder, it.fradato, it.tildato)
        }
    }


    private fun beregnBeløpPeriode(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        val (startDato, sluttDato, inntekt, samordningsfradrag) = inntektsperiode
        return finnGrunnbeløpsPerioder(startDato, sluttDato).map {
            val avkortning = avkortningPerMåned(it.beløp, inntekt)
            val fullOvergangsStønad =
                    it.beløp.multiply(BigDecimal(2.25))

            val utbetaling = fullOvergangsStønad.subtract(avkortning).divide(BigDecimal(12)).subtract(samordningsfradrag)
                    .setScale(0, RoundingMode.HALF_UP)

            Beløpsperiode(fraOgMedDato = it.fraOgMedDato,
                          tilDato = it.tilDato,
                          beløp = utbetaling,
                          beregningsgrunnlag = Beregningsgrunnlag(samordningsfradrag = samordningsfradrag,
                                                                  inntekt = inntekt,
                                                                  grunnbeløp = it.beløp))
        }
    }

    private fun avkortningPerMåned(grunnbeløp: BigDecimal, inntekt: BigDecimal): BigDecimal {
        val inntektOverHalveGrunnbeløp = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
        return if (inntektOverHalveGrunnbeløp > BigDecimal(0))
            inntektOverHalveGrunnbeløp.multiply(BigDecimal(0.45)).setScale(5, RoundingMode.HALF_DOWN) else BigDecimal(0)
    }


    private fun finnGjeldeneBeløpsperiode(beløpForInnteksperioder: List<Beløpsperiode>,
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
        }
    }

}