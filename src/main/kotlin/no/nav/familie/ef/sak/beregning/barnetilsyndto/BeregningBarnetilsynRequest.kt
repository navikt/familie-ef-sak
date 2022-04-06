package no.nav.familie.ef.sak.beregning.barnetilsyndto

import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class BeregningBarnetilsynRequest(
        val utgiftsperioder: List<UtgiftsperiodeDto>,
        val kontantstøtteperioder: List<KontantstøttePeriodeDto>,
        val tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>
)

data class TilleggsstønadPeriodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val beløp: BigDecimal
)

data class KontantstøttePeriodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val beløp: BigDecimal
)

data class UtgiftsperiodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val barn: List<UUID>,
        val utgifter: BigDecimal // Totalutgift eller utgift per måned
)

data class UtgiftsMåned(
        val årMåned : YearMonth,
        val barn: List<UUID>,
        val utgifter: BigDecimal
)
