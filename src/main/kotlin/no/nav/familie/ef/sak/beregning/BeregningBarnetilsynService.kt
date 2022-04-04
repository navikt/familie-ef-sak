package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningBarnetilsynService {

    fun beregnYtelseBarnetilsyn(utgiftsperioder: List<UtgiftsperiodeDto>,
                                kontantstøttePerioder: List<KontantstøttePeriodeDto>,
                                tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>): List<BeløpsperiodeBarnetilsynDto> {


        val barnetilsynMåneder = utgiftsperioder.map {
            it.split()
        }.flatMap {
            it.map { utgiftsMåned ->
                lagUtgiftsmåned(utgiftsMåned, kontantstøttePerioder, tilleggsstønadsperioder)
            }
        }
        return barnetilsynMåneder.merge()
    }

    private fun lagUtgiftsmåned(utgiftsMåned: UtgiftsMåned,
                                kontantstøttePerioder: List<KontantstøttePeriodeDto>,
                                tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>): BeløpsperiodeBarnetilsynDto {
        val kontantStøtteBeløp: BigDecimal =
                kontantstøttePerioder.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
                ?: BigDecimal.ZERO
        val tilleggsstønadsperiodeBeløp =
                tilleggsstønadsperioder.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
                ?: BigDecimal.ZERO

        return BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(utgiftsMåned,
                                                                    kontantStøtteBeløp,
                                                                    tilleggsstønadsperiodeBeløp,
                                                                    BeregningBarnetilsynUtil.satserForBarnetilsyn.hentSatsFor(
                                                                            utgiftsMåned.barn.size, utgiftsMåned.årMåned))
    }
}

fun UtgiftsperiodeDto.split(): List<UtgiftsMåned> {
    val perioder = mutableListOf<UtgiftsMåned>()
    var måned = this.årMånedFra
    while (måned.isBefore(this.årMånedTil)) {
        perioder.add(UtgiftsMåned(måned, this.barn, this.utgifter))
        måned.plusMonths(1)
    }
    return perioder
}

fun List<BeløpsperiodeBarnetilsynDto>.merge(): List<BeløpsperiodeBarnetilsynDto> {

    val sortedBy = this.sortedBy { it.periode.fradato }
    var beløpsPeriodeDto = sortedBy.first()
    var tempPeriode = beløpsPeriodeDto.periode
    val mergedeBeløpsperioder = mutableListOf<BeløpsperiodeBarnetilsynDto>()

    sortedBy.forEach {
        if (it.beløp == beløpsPeriodeDto.beløp && it.beregningsgrunnlag == beløpsPeriodeDto.beregningsgrunnlag) {
            tempPeriode = tempPeriode.copy(tildato = it.periode.tildato)
        } else {
            mergedeBeløpsperioder.add(beløpsPeriodeDto.copy(periode = tempPeriode))
            beløpsPeriodeDto = it
            tempPeriode = beløpsPeriodeDto.periode
        }
    }
    if (!mergedeBeløpsperioder.contains(beløpsPeriodeDto)) {
        mergedeBeløpsperioder.add(beløpsPeriodeDto.copy(periode = tempPeriode))
    }
    return mergedeBeløpsperioder
}
