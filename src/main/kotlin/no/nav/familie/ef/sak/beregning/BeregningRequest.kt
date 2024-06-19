package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BeregningRequest(
    val inntekt: List<Inntekt>,
    val vedtaksperioder: List<VedtaksperiodeDto>,
)

data class Inntekt(
    val årMånedFra: YearMonth,
    val forventetInntekt: BigDecimal?,
    val samordningsfradrag: BigDecimal?,
    val dagsats: BigDecimal? = null,
    val månedsinntekt: BigDecimal? = null,
)

// TODO Dette er en domeneklasse og burde flyttes til Vedtak.kt.
data class Inntektsperiode(
    @Deprecated("Bruk periode", ReplaceWith("periode.fom")) val startDato: LocalDate? = null,
    @Deprecated("Bruk periode", ReplaceWith("periode.tom")) val sluttDato: LocalDate? = null,
    val periode: Månedsperiode =
        Månedsperiode(
            startDato ?: error("periode eller startDato må ha verdi"),
            sluttDato ?: error("periode eller sluttDato må ha verdi"),
        ),
    val dagsats: BigDecimal? = null,
    val månedsinntekt: BigDecimal? = null,
    val inntekt: BigDecimal,
    val samordningsfradrag: BigDecimal,
) {
    fun totalinntekt(): BigDecimal =
        this.inntekt +
            (this.dagsats ?: BigDecimal.ZERO).multiply(BeregningUtils.DAGSATS_ANTALL_DAGER) +
            (this.månedsinntekt ?: BigDecimal.ZERO).multiply(BeregningUtils.ANTALL_MÅNEDER_ÅR)

    /**
     * Dersom den eksisterende årsinntekten er g-omregnet til nærmeste 100,
     * så skal vi ikke nedjustere denne til nærmeste 1000
     */
    fun skalRundeAvTilNærmeste1000(): Boolean {
        if (this.dagsats != null && this.dagsats > BigDecimal.ZERO) {
            return true
        } else if (this.månedsinntekt != null && this.månedsinntekt > BigDecimal.ZERO) {
            return true
        } else if (this.inntekt.remainder(BigDecimal(100)) > BigDecimal.ZERO) {
            return true
        } else {
            return false
        }
    }
}

fun List<Inntekt>.tilInntektsperioder() =
    this.mapIndexed { index, inntektsperiode ->
        Inntektsperiode(
            dagsats = inntektsperiode.dagsats ?: BigDecimal.ZERO,
            månedsinntekt = inntektsperiode.månedsinntekt ?: BigDecimal.ZERO,
            inntekt = inntektsperiode.forventetInntekt ?: BigDecimal.ZERO,
            samordningsfradrag = inntektsperiode.samordningsfradrag ?: BigDecimal.ZERO,
            periode =
                Månedsperiode(
                    fom = inntektsperiode.årMånedFra,
                    tom =
                        if (index < this.lastIndex && this.size > 1) {
                            this[index + 1].årMånedFra.minusMonths(1)
                        } else {
                            YearMonth.from(LocalDate.MAX)
                        },
                ),
        )
    }

fun List<Inntektsperiode>.tilInntekt() =
    this.map { inntektsperiode ->
        Inntekt(
            dagsats = inntektsperiode.dagsats,
            månedsinntekt = inntektsperiode.månedsinntekt,
            forventetInntekt = inntektsperiode.inntekt,
            samordningsfradrag = inntektsperiode.samordningsfradrag,
            årMånedFra = inntektsperiode.periode.fom,
        )
    }
