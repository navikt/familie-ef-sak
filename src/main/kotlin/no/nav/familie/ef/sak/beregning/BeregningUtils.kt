package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.felles.util.Utregning.rundNedTilNærmeste100
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.YearMonth

object BeregningUtils {

    private val REDUKSJONSFAKTOR = BigDecimal(0.45)
    private val DAGSATS_ANTALL_DAGER = BigDecimal(260)
    private val ANTALL_MÅNEDER_ÅR = BigDecimal(12)

    fun beregnStønadForInntekt(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        val periode = inntektsperiode.periode
        val samordningsfradrag = inntektsperiode.samordningsfradrag
        val totalInntekt = beregnTotalinntekt(inntektsperiode)
        return finnGrunnbeløpsPerioder(periode).map {
            val avkortningPerMåned = beregnAvkortning(it.beløp, totalInntekt).divide(ANTALL_MÅNEDER_ÅR)
                .setScale(0, RoundingMode.HALF_DOWN)

            val fullOvergangsStønadPerMåned =
                it.beløp.multiply(BigDecimal(2.25)).divide(ANTALL_MÅNEDER_ÅR).setScale(0, RoundingMode.HALF_EVEN)

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
                    inntekt = totalInntekt,
                    grunnbeløp = it.beløp
                )
            )
        }
    }

    private fun beregnTotalinntekt(inntektsperiode: Inntektsperiode): BigDecimal {
        val totalInntekt = inntektsperiode.inntekt +
            (inntektsperiode.dagsats ?: BigDecimal.ZERO).multiply(DAGSATS_ANTALL_DAGER) +
            (inntektsperiode.månedsinntekt ?: BigDecimal.ZERO).multiply(ANTALL_MÅNEDER_ÅR)
        // rund ned
        return totalInntekt
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
        val inntekt = inntektsperiode.inntekt // TODO skal beregne på totalinntekten ?
        val samordningsfradrag = inntektsperiode.samordningsfradrag
        return finnGrunnbeløpsPerioder(inntektsperiode.periode).map { grunnbeløp ->
            if (grunnbeløp.periode.fom > sistBrukteGrunnbeløp.periode.fom &&
                grunnbeløp.beløp != sistBrukteGrunnbeløp.grunnbeløp
            ) {
                val faktor = grunnbeløp.beløp.divide(sistBrukteGrunnbeløp.grunnbeløp, MathContext.DECIMAL128)
                val justertInntekt = inntekt.multiply(faktor)
                val justerInntektAvrundetNedTilNærmeste100 = rundNedTilNærmeste100(justertInntekt) // hvorfor runde ned till 100 her og ikke 1000?
                Inntektsperiode(
                    periode = grunnbeløp.periode,
                    inntekt = BigDecimal(justerInntektAvrundetNedTilNærmeste100),
                    samordningsfradrag = samordningsfradrag
                )
            } else {
                Inntektsperiode(periode = grunnbeløp.periode, inntekt = inntekt, samordningsfradrag = samordningsfradrag)
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
