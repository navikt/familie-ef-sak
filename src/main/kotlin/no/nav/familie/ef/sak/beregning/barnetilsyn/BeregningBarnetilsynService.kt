package no.nav.familie.ef.sak.beregning.barnetilsyn

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class BeregningBarnetilsynService {

    fun beregnYtelseBarnetilsyn(utgiftsperioder: List<UtgiftsperiodeDto>,
                                kontantstøttePerioder: List<KontantstøttePeriodeDto>,
                                tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>): List<BeløpsperiodeBarnetilsynDto> {

        val barnetilsynMåneder = utgiftsperioder.map {
            it.split()
        }.flatMap {
            it.map { utgiftsMåned ->
                utgiftsMåned.tilBeløpsperiodeBarnetilsynDto(kontantstøttePerioder, tilleggsstønadsperioder)
            }
        }
        return barnetilsynMåneder.mergeSammenhengendePerioder()
    }
}

/**
 * Del opp utgiftsperioder i atomiske deler (mnd).
 * Eksempel: 1stk UtgiftsperiodeDto fra januar til mars deles opp i 3:
 * listOf(UtgiftsMåned(jan), UtgiftsMåned(feb), UtgiftsMåned(mars))
 */
fun UtgiftsperiodeDto.split(): List<UtgiftsMåned> {
    val perioder = mutableListOf<UtgiftsMåned>()
    var måned = this.årMånedFra
    while (måned <= this.årMånedTil) {
        perioder.add(UtgiftsMåned(måned, this.barn, this.utgifter))
        måned = måned.plusMonths(1)
    }
    return perioder
}

/**
 * Merger sammenhengende perioder hvor beløp og @BeløpsperiodeBarnetilsynDto#beregningsgrunnlag (it.toKey()) er like.
 */
fun List<BeløpsperiodeBarnetilsynDto>.mergeSammenhengendePerioder(): List<BeløpsperiodeBarnetilsynDto> {
    val sortertPåDatoListe = this.sortedBy { it.periode.fradato }
    return sortertPåDatoListe.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()
        if (last != null && erSammenhengendeMedSammeBeløpOgBeregningsgrunnlag(last, entry)) {
            acc.removeLast()
            acc.add(last.copy(periode = last.periode.copy(tildato = entry.periode.tildato)))
        } else {
            acc.add(entry)
        }
        acc
    }
}

fun erSammenhengendeMedSammeBeløpOgBeregningsgrunnlag(first: BeløpsperiodeBarnetilsynDto,
                                                      second: BeløpsperiodeBarnetilsynDto): Boolean {
    val firstDatePlussEnMnd = first.periode.tildato.plusMonths(1)
    return YearMonth.from(firstDatePlussEnMnd) == YearMonth.from(second.periode.fradato) &&
           first.toKey() == second.toKey()
}

private fun UtgiftsMåned.tilBeløpsperiodeBarnetilsynDto(kontantstøttePerioder: List<KontantstøttePeriodeDto>,
                                                        tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>): BeløpsperiodeBarnetilsynDto {
    val kontantStøtteBeløp = kontantstøttePerioder.finnKontantstøtteBeløp(this)
    val tilleggsstønadsperiodeBeløp = tilleggsstønadsperioder.finnTillegstønadBeløp(this)

    return BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(utgiftsperiode = this,
                                                                kontantstøtteBeløp = kontantStøtteBeløp,
                                                                tilleggsstønadBeløp = tilleggsstønadsperiodeBeløp,
                                                                antallBarnIPeriode = this.barn.size)
}

private fun BeløpsperiodeBarnetilsynDto.toKey() = Key(this.beløp.toInt(), this.beregningsgrunnlag)

private fun List<TilleggsstønadPeriodeDto>.finnTillegstønadBeløp(utgiftsMåned: UtgiftsMåned): BigDecimal {
    return this.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
           ?: BigDecimal.ZERO
}

private fun List<KontantstøttePeriodeDto>.finnKontantstøtteBeløp(utgiftsMåned: UtgiftsMåned): BigDecimal {
    return this.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
           ?: BigDecimal.ZERO
}

data class Key(val beløp: Int, val grunnlag: BeregningsgrunnlagBarnetilsynDto)