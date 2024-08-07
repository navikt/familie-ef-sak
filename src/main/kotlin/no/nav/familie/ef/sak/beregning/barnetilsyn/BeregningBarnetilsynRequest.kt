package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

data class BeregningBarnetilsynRequest(
    val utgiftsperioder: List<UtgiftsperiodeDto>,
    val kontantstøtteperioder: List<PeriodeMedBeløpDto>,
    val tilleggsstønadsperioder: List<PeriodeMedBeløpDto>,
)

data class UtgiftsMåned(
    val årMåned: YearMonth,
    val barn: List<UUID>,
    val utgifter: BigDecimal,
    val aktivitetstype: AktivitetstypeBarnetilsyn?,
    val periodetype: PeriodetypeBarnetilsyn,
)

fun UtgiftsMåned.tilBeløpsperiodeBarnetilsynDto(
    kontantstøttePerioder: List<PeriodeMedBeløpDto>,
    tilleggsstønadsperioder: List<PeriodeMedBeløpDto>,
    brukIkkeVedtatteSatser: Boolean,
): BeløpsperiodeBarnetilsynDto {
    val kontantStøtteBeløp = kontantstøttePerioder.finnPeriodeBeløp(this)
    val tilleggsstønadsperiodeBeløp = tilleggsstønadsperioder.finnPeriodeBeløp(this)

    return BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(
        utgiftsperiode = this,
        kontantstøtteBeløp = BigDecimal(kontantStøtteBeløp),
        tilleggsstønadBeløp = BigDecimal(tilleggsstønadsperiodeBeløp),
        barn = this.barn,
        brukIkkeVedtatteSatser = brukIkkeVedtatteSatser,
    )
}

private fun List<PeriodeMedBeløpDto>.finnPeriodeBeløp(utgiftsMåned: UtgiftsMåned): Int = this.find { it.periode.inneholder(utgiftsMåned.årMåned) }?.beløp ?: 0
