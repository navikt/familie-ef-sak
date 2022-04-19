package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class BeregningBarnetilsynRequest(
        val utgiftsperioder: List<UtgiftsperiodeDto>,
        val kontantstøtteperioder: List<PeriodeMedBeløpDto>,
        val tilleggsstønadsperioder: List<PeriodeMedBeløpDto>
)


data class UtgiftsMåned(
        val årMåned: YearMonth,
        val barn: List<UUID>,
        val utgifter: BigDecimal
)

fun UtgiftsMåned.tilBeløpsperiodeBarnetilsynDto(kontantstøttePerioder: List<PeriodeMedBeløpDto>,
                                                tilleggsstønadsperioder: List<PeriodeMedBeløpDto>): BeløpsperiodeBarnetilsynDto {
    val kontantStøtteBeløp = kontantstøttePerioder.finnPeriodeBeløp(this)
    val tilleggsstønadsperiodeBeløp = tilleggsstønadsperioder.finnPeriodeBeløp(this)

    return BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(utgiftsperiode = this,
                                                                kontantstøtteBeløp = BigDecimal(kontantStøtteBeløp),
                                                                tilleggsstønadBeløp = BigDecimal(tilleggsstønadsperiodeBeløp),
                                                                antallBarnIPeriode = this.barn.size)
}

private fun List<PeriodeMedBeløpDto>.finnPeriodeBeløp(utgiftsMåned: UtgiftsMåned): Int {
    return this.find { utgiftsMåned.omsluttesAv(it) }?.beløp ?: 0
}

private fun UtgiftsMåned.omsluttesAv(it: PeriodeMedBeløpDto) = this.årMåned.omsluttesAv(it.årMånedFra, it.årMånedTil)

