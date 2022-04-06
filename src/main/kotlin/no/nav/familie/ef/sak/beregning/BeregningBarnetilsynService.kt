package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.felles.dto.Periode
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
    while (måned.isBefore(this.årMånedTil) || måned.equals(this.årMånedTil)) {
        perioder.add(UtgiftsMåned(måned, this.barn, this.utgifter))
        måned = måned.plusMonths(1)
    }
    return perioder
}

/**
 * Merger sammenhengende perioder hvor beløp og @BeløpsperiodeBarnetilsynDto#beregningsgrunnlag (it.toKey()) er like.
 */
fun List<BeløpsperiodeBarnetilsynDto>.mergeSammenhengendePerioder(): List<BeløpsperiodeBarnetilsynDto> {
    return mapNotNull { it }.groupingBy { it.toKey() }
            .aggregate { _, akkumulatorListe: MutableList<BeløpsperiodeBarnetilsynDto>?, nestePeriodeDto, first ->
                when (first) {
                    true -> mutableListOf(nestePeriodeDto)
                    false -> oppdaterEllerLeggTilNy(akkumulatorListe, nestePeriodeDto)
                }
            }.values.flatten()
}

private fun oppdaterEllerLeggTilNy(akkumulatorListe: MutableList<BeløpsperiodeBarnetilsynDto>?,
                                   nestePeriodeDto: BeløpsperiodeBarnetilsynDto) =
        when (erSammenhengende(akkumulatorListe!!.last().periode, nestePeriodeDto.periode)) {
            true -> lagNyTildato(akkumulatorListe, nestePeriodeDto)
            false -> akkumulatorListe.leggTil(nestePeriodeDto)
        }

private fun lagNyTildato(akkumulatorListe: MutableList<BeløpsperiodeBarnetilsynDto>,
                         nestePeriodeDto: BeløpsperiodeBarnetilsynDto): MutableList<BeløpsperiodeBarnetilsynDto> {
    val oppdatertBeløpsperiodeKopi = lagKopiMedNyTildato(akkumulatorListe.last(), nestePeriodeDto)
    return akkumulatorListe.byttUtSisteMed(oppdatertBeløpsperiodeKopi)
}

private fun lagKopiMedNyTildato(beløpsperiodeBarnetilsynDto: BeløpsperiodeBarnetilsynDto,
                                nestePeriodeDto: BeløpsperiodeBarnetilsynDto): BeløpsperiodeBarnetilsynDto {
    val nyPeriode = beløpsperiodeBarnetilsynDto.periode.copy(tildato = nestePeriodeDto.periode.tildato)
    return beløpsperiodeBarnetilsynDto.copy(periode = nyPeriode)
}

private fun erSammenhengende(gjeldendePeriode: Periode,
                             nestePeriode: Periode) =
        gjeldendePeriode.tildato.month.equals(nestePeriode.fradato.minusMonths(1).month)

private fun UtgiftsMåned.tilBeløpsperiodeBarnetilsynDto(kontantstøttePerioder: List<KontantstøttePeriodeDto>,
                                                        tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>): BeløpsperiodeBarnetilsynDto {
    val kontantStøtteBeløp = kontantstøttePerioder.finnKontantstøtteBeløp(this)
    val tilleggsstønadsperiodeBeløp = tilleggsstønadsperioder.finnTillegstønadBeløp(this)

    return BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(utgiftsperiode = this,
                                                                kontantstøtteBeløp = kontantStøtteBeløp,
                                                                tilleggsstønadBeløp = tilleggsstønadsperiodeBeløp,
                                                                antallBarnIPeriode = this.barn.size)
}

private fun <E> MutableList<E>.leggTil(nestePeriodeDto: E): MutableList<E> {
    this.add(nestePeriodeDto)
    return this
}

private fun <E> MutableList<E>.byttUtSisteMed(ny: E): MutableList<E> {
    this.removeLast()
    this.add(ny)
    return this
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