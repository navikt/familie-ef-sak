package no.nav.familie.ef.sak.beregning

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class ÅrMånedPeriode(val årMånedFra: YearMonth, val årMånedTil: YearMonth)

data class BeregningRequest(val inntekt: List<Inntekt>, val vedtaksperioder: List<ÅrMånedPeriode>)

data class Inntekt(val årMånedFra: YearMonth, val forventetInntekt: BigDecimal?, val samordningsfradrag: BigDecimal?)
data class Inntektsperiode(val startDato: LocalDate,
                           val sluttDato: LocalDate,
                           val inntekt: BigDecimal,
                           val samordningsfradrag: BigDecimal)


fun List<Inntekt>.tilInntektsperioder() = this.mapIndexed { index, inntektsperiode ->
    Inntektsperiode(inntekt = inntektsperiode.forventetInntekt ?: BigDecimal.ZERO,
                    samordningsfradrag = inntektsperiode.samordningsfradrag ?: BigDecimal.ZERO,
                    startDato = inntektsperiode.årMånedFra.atDay(1),
                    sluttDato = if (index < this.lastIndex && this.size > 1) this[index + 1].årMånedFra.atDay(1)
                            .minusDays(1) else LocalDate.MAX)
}

fun List<Inntektsperiode>.tilInntekt() = this.mapIndexed { index, inntektsperiode ->
    Inntekt(forventetInntekt = inntektsperiode.inntekt,
            samordningsfradrag = inntektsperiode.samordningsfradrag,
            årMånedFra = YearMonth.from(inntektsperiode.startDato))
}
