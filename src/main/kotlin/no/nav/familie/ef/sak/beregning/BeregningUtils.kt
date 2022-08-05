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
                periode = it.periode,
                fellesperiode = it.fellesperiode,
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
            if (grunnbeløp.fellesperiode.fom > sistBrukteGrunnbeløp.periode.fom &&
                grunnbeløp.beløp != sistBrukteGrunnbeløp.grunnbeløp
            ) {
                val faktor = grunnbeløp.beløp.divide(sistBrukteGrunnbeløp.grunnbeløp, MathContext.DECIMAL128)
                val justerInntekt = inntekt.multiply(faktor).setScale(0, RoundingMode.FLOOR).toLong()
                val justerInntektAvrundetNedTilNærmeste100 = (justerInntekt / 100L) * 100L
                Inntektsperiode(
                    grunnbeløp.fellesperiode.toMånedsperiode(),
                    BigDecimal(justerInntektAvrundetNedTilNærmeste100),
                    samordningsfradrag
                )
            } else {
                Inntektsperiode(grunnbeløp.fellesperiode.toMånedsperiode(), inntekt, samordningsfradrag)
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
                it.fellesperiode.omsluttesAv(vedtaksdatoperiode) -> {
                    it
                }
                it.fellesperiode.overlapperIStartenAv(vedtaksdatoperiode) -> {
                    it.copy(
                        periode = it.periode.copy(fradato = vedtaksdatoperiode.fom),
                        fellesperiode = (it.fellesperiode snitt vedtaksdatoperiode)!!
                    )
                }
                vedtaksdatoperiode.overlapperIStartenAv(it.fellesperiode) -> {
                    it.copy(
                        periode = it.periode.copy(tildato = vedtaksdatoperiode.tom),
                        fellesperiode = (it.fellesperiode snitt vedtaksdatoperiode)!!
                    )
                }
                vedtaksdatoperiode.omsluttesAv(it.fellesperiode) -> {
                    it.copy(
                        periode = it.periode.copy(fradato = vedtaksdatoperiode.fom, tildato = vedtaksdatoperiode.tom),
                        fellesperiode = (it.fellesperiode snitt vedtaksdatoperiode)!!
                    )
                }
                else -> {
                    null
                }
            }
        }
    }
}
