package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.felles.util.Utregning.rundNedTilNærmeste100
import no.nav.familie.ef.sak.felles.util.Utregning.rundNedTilNærmeste1000
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.MathContext
import java.math.RoundingMode
import java.time.YearMonth

object BeregningUtils {
    private val REDUKSJONSFAKTOR = BigDecimal(0.45)
    val DAGSATS_ANTALL_DAGER = BigDecimal(260)
    val ANTALL_MÅNEDER_ÅR = BigDecimal(12)

    fun beregnStønadForInntekt(inntektsperiode: Inntektsperiode): List<Beløpsperiode> {
        val periode = inntektsperiode.periode
        val samordningsfradrag = inntektsperiode.samordningsfradrag
        val totalInntekt = beregnTotalinntekt(inntektsperiode)

        return finnGrunnbeløpsPerioder(periode).map {
            val avkortningPerMåned =
                beregnAvkortning(it.beløp, totalInntekt)
                    .divide(ANTALL_MÅNEDER_ÅR)
                    .setScale(0, RoundingMode.HALF_DOWN)

            val fullOvergangsStønadPerMåned =
                it.beløp
                    .multiply(BigDecimal(2.25))
                    .divide(ANTALL_MÅNEDER_ÅR)
                    .setScale(0, RoundingMode.HALF_EVEN)

            val beløpFørSamordningUtenAvrunding =
                fullOvergangsStønadPerMåned.subtract(avkortningPerMåned).setScale(0, RoundingMode.HALF_UP)

            val utbetaling =
                beløpFørSamordningUtenAvrunding.subtract(samordningsfradrag).setScale(0, RoundingMode.HALF_UP)

            val beløpFørSamordning =
                if (beløpFørSamordningUtenAvrunding <= ZERO) ZERO else beløpFørSamordningUtenAvrunding

            val beløpTilUtbetalning = if (utbetaling <= ZERO) ZERO else utbetaling

            Beløpsperiode(
                periode = it.periode,
                beløp = beløpTilUtbetalning,
                beløpFørSamordning = beløpFørSamordning,
                beregningsgrunnlag =
                    Beregningsgrunnlag(
                        samordningsfradrag = samordningsfradrag,
                        avkortningPerMåned = avkortningPerMåned,
                        fullOvergangsStønadPerMåned = fullOvergangsStønadPerMåned,
                        inntekt = totalInntekt,
                        grunnbeløp = it.beløp,
                    ),
            )
        }
    }

    fun beregnTotalinntekt(inntektsperiode: Inntektsperiode): BigDecimal {
        val totalInntekt = inntektsperiode.totalinntekt()
        val skalRundeNedBasertPåInntektsperiodensVerdier = inntektsperiode.skalRundeAvTilNærmeste1000()

        return if (skalRundeNedBasertPåInntektsperiodensVerdier) BigDecimal(rundNedTilNærmeste1000(totalInntekt)) else totalInntekt
    }

    fun beregn10ProsentOppOgNedIMånedsinntektFraÅrsinntekt(inntektsperiode: Inntektsperiode): TiProsentOppOgNed {
        val årsinntekt = beregnTotalinntekt(inntektsperiode)
        if (inntektsperiode.inntekt == ZERO) {
            val grunnbeløp = Grunnbeløpsperioder.nyesteGrunnbeløp.perMnd
            return TiProsentOppOgNed(grunnbeløp.toInt() / (2), 0)
        }
        val månedInntekt10ProsentOppOgNed = TiProsentOppOgNed(((årsinntekt.toDouble() / 12) * 1.1).toInt(), ((årsinntekt.toDouble() / 12) * 0.9).toInt())
        return månedInntekt10ProsentOppOgNed
    }

    data class TiProsentOppOgNed(
        val opp: Int,
        val ned: Int,
    )

    private fun beregnAvkortning(
        grunnbeløp: BigDecimal,
        inntekt: BigDecimal,
    ): BigDecimal {
        val inntektOverHalveGrunnbeløp = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
        return if (inntektOverHalveGrunnbeløp > ZERO) {
            inntektOverHalveGrunnbeløp.multiply(REDUKSJONSFAKTOR).setScale(5, RoundingMode.HALF_DOWN)
        } else {
            ZERO
        }
    }

    fun indeksjusterInntekt(
        sisteBrukteGrunnbeløpsdato: YearMonth,
        inntekter: List<Inntektsperiode> = emptyList(),
    ): List<Inntektsperiode> {
        val sistBrukteGrunnbeløp = Grunnbeløpsperioder.finnGrunnbeløp(sisteBrukteGrunnbeløpsdato)
        if (Grunnbeløpsperioder.nyesteGrunnbeløp == sistBrukteGrunnbeløp) {
            return inntekter
        }

        return inntekter.flatMap { justerInntektsperiode(it, sistBrukteGrunnbeløp) }
    }

    private fun justerInntektsperiode(
        inntektsperiode: Inntektsperiode,
        sistBrukteGrunnbeløp: Grunnbeløp,
    ): List<Inntektsperiode> {
        val samordningsfradrag = inntektsperiode.samordningsfradrag

        return finnGrunnbeløpsPerioder(inntektsperiode.periode).map { grunnbeløp ->

            if (grunnbeløp.periode.fom > sistBrukteGrunnbeløp.periode.fom &&
                grunnbeløp.beløp != sistBrukteGrunnbeløp.grunnbeløp
            ) {
                val faktor = grunnbeløp.beløp.divide(sistBrukteGrunnbeløp.grunnbeløp, MathContext.DECIMAL128)

                // Her velger vi å tolke alle inntekter (totalinntekt) som "uavrundet inntekter" (reell).
                // Vi runder derfor ned til nærmeste 1000 før vi justerer inntekt.
                // Alternativ er "ikke rund ned g-beløp til nærmeste 1000" - gir potensielt feil ved
                // eksisterende beløp som tilfeldigvis er 100 (pre - warning)

                val totalinntekt = inntektsperiode.totalinntekt()
                val f = rundNedTilNærmeste1000(totalinntekt)

                val indeksjustertInntektF = rundNedTilNærmeste100(faktor.multiply(f.toBigDecimal()))

                // Sletter inntekt-data som saksbehandler har lagt inn (dag/mnd) og legger inn justert inntekt.
                inntektsperiode.copy(
                    dagsats = ZERO,
                    månedsinntekt = ZERO,
                    inntekt = indeksjustertInntektF,
                    periode = grunnbeløp.periode,
                )
            } else {
                Inntektsperiode(
                    periode = grunnbeløp.periode,
                    dagsats = inntektsperiode.dagsats,
                    månedsinntekt = inntektsperiode.månedsinntekt,
                    inntekt = inntektsperiode.inntekt,
                    samordningsfradrag = samordningsfradrag,
                )
            }
        }
    }

    fun finnStartDatoOgSluttDatoForBeløpsperiode(
        beløpForInnteksperioder: List<Beløpsperiode>,
        vedtaksperiode: Månedsperiode,
    ): List<Beløpsperiode> =
        beløpForInnteksperioder.mapNotNull {
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
