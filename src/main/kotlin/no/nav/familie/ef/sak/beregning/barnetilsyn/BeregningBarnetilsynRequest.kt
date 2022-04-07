package no.nav.familie.ef.sak.beregning.barnetilsyn

import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class BeregningBarnetilsynRequest(
        val utgiftsperioder: List<UtgiftsperiodeDto>,
        val kontantstøtteperioder: List<PeriodeMedBeløpDto>,
        val tilleggsstønadsperioder: List<PeriodeMedBeløpDto>
)

data class PeriodeMedBeløpDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val beløp: BigDecimal
)

data class UtgiftsperiodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val barn: List<UUID>,
        val utgifter: BigDecimal
)

data class UtgiftsMåned(
        val årMåned : YearMonth,
        val barn: List<UUID>,
        val utgifter: BigDecimal
)
