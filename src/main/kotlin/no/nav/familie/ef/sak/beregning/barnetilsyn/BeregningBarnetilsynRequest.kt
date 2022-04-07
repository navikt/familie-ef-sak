package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.felles.dto.Periode
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
) {
    fun tilPeriode(): Periode = Periode(this.årMånedFra.atDay(1), this.årMånedTil.atEndOfMonth())
}

fun List<UtgiftsperiodeDto>.tilPerioder(): List<Periode> =
        this.map {
            it.tilPeriode()
        }


data class UtgiftsMåned(
        val årMåned : YearMonth,
        val barn: List<UUID>,
        val utgifter: BigDecimal
)
