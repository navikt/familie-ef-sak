package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BeregningRequest(val inntekt: List<Inntekt>, val vedtaksperioder: List<VedtaksperiodeDto>)

data class Inntekt(val årMånedFra: YearMonth, val forventetInntekt: BigDecimal?, val samordningsfradrag: BigDecimal?)

// TODO Dette er en domeneklasse og burde flyttes til Vedtak.kt.
data class Inntektsperiode(
    @Deprecated("Bruk periode", ReplaceWith("periode.fom")) val startDato: LocalDate? = null,
    @Deprecated("Bruk periode", ReplaceWith("periode.tom")) val sluttDato: LocalDate? = null,
    val periode: Månedsperiode =
        Månedsperiode(
            startDato ?: error("periode eller startDato må ha verdi"),
            sluttDato ?: error("periode eller sluttDato må ha verdi"),
        ),
    val inntekt: BigDecimal,
    val samordningsfradrag: BigDecimal,
)

fun List<Inntekt>.tilInntektsperioder() = this.mapIndexed { index, inntektsperiode ->
    Inntektsperiode(
        inntekt = inntektsperiode.forventetInntekt ?: BigDecimal.ZERO,
        samordningsfradrag = inntektsperiode.samordningsfradrag ?: BigDecimal.ZERO,
        periode = Månedsperiode(
            fom = inntektsperiode.årMånedFra,
            tom = if (index < this.lastIndex && this.size > 1) {
                this[index + 1].årMånedFra.minusMonths(1)
            } else {
                YearMonth.from(LocalDate.MAX)
            },
        ),
    )
}

fun List<Inntektsperiode>.tilInntekt() = this.map { inntektsperiode ->
    Inntekt(
        forventetInntekt = inntektsperiode.inntekt,
        samordningsfradrag = inntektsperiode.samordningsfradrag,
        årMånedFra = inntektsperiode.periode.fom,
    )
}
