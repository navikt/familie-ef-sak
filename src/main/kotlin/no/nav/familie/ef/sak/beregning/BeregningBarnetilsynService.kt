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
                lagUtgiftsmåned(utgiftsMåned, kontantstøttePerioder, tilleggsstønadsperioder)
            }
        }
        return barnetilsynMåneder.merge()
    }

    private fun lagUtgiftsmåned(utgiftsMåned: UtgiftsMåned,
                                kontantstøttePerioder: List<KontantstøttePeriodeDto>,
                                tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>): BeløpsperiodeBarnetilsynDto {
        val kontantStøtteBeløp = kontantstøttePerioder.finnKontantstøtteBeløp(utgiftsMåned)
        val tilleggsstønadsperiodeBeløp = tilleggsstønadsperioder.finnTillegstønadBeløp(utgiftsMåned)

        return BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(utgiftsperiode = utgiftsMåned,
                                                                    kontantstøtteBeløp = kontantStøtteBeløp,
                                                                    tilleggsstønadBeløp = tilleggsstønadsperiodeBeløp,
                                                                    antallBarnIPeriode = utgiftsMåned.barn.size)
    }
}

fun UtgiftsperiodeDto.split(): List<UtgiftsMåned> {
    val perioder = mutableListOf<UtgiftsMåned>()
    var måned = this.årMånedFra
    while (måned.isBefore(this.årMånedTil) || måned.equals(this.årMånedTil)) {
        perioder.add(UtgiftsMåned(måned, this.barn, this.utgifter))
        måned = måned.plusMonths(1)
    }
    return perioder
}

fun List<BeløpsperiodeBarnetilsynDto>.merge(): List<BeløpsperiodeBarnetilsynDto> {
    return mapNotNull { it }.groupingBy { it.toKey() }
            .aggregate { _, akkumulatorListe: MutableList<BeløpsperiodeBarnetilsynDto>?, nestePeriodeDto, first ->
                if (first) {
                    mutableListOf(nestePeriodeDto)
                } else if (erSammenhengende(akkumulatorListe!!.last().periode, nestePeriodeDto.periode)) {
                    val oppdatertBeløpsperiodeKopi = lagKopiMedNyTildato(akkumulatorListe.last(), nestePeriodeDto)
                    akkumulatorListe.byttUtSisteMed(oppdatertBeløpsperiodeKopi)
                } else {
                    akkumulatorListe.leggTil(nestePeriodeDto)
                }
            }.values.flatten()
}

private fun <E> MutableList<E>.leggTil(nestePeriodeDto: E): MutableList<E> {
    this.add(nestePeriodeDto)
    return this
}

private fun <E> MutableList<E>.byttUtSisteMed(copy: E): MutableList<E> {
    this.removeLast()
    this.add(copy)
    return this
}

private fun lagKopiMedNyTildato(beløpsperiodeBarnetilsynDto: BeløpsperiodeBarnetilsynDto,
                                nestePeriodeDto: BeløpsperiodeBarnetilsynDto): BeløpsperiodeBarnetilsynDto {
    val nyPeriode = beløpsperiodeBarnetilsynDto.periode.copy(tildato = nestePeriodeDto.periode.tildato)
    val copy = beløpsperiodeBarnetilsynDto.copy(periode = nyPeriode)
    return copy
}

private fun erSammenhengende(gjeldendePeriode: Periode,
                             nestePeriode: Periode) =
        gjeldendePeriode.tildato.month.equals(nestePeriode.fradato.minusMonths(1).month)

data class Key(val beløp: Int, val grunnlag: BeregningsgrunnlagBarnetilsynDto)

private fun BeløpsperiodeBarnetilsynDto.toKey() = Key(this.beløp.toInt(), this.beregningsgrunnlag)

private fun List<TilleggsstønadPeriodeDto>.finnTillegstønadBeløp(utgiftsMåned: UtgiftsMåned): BigDecimal {
    return this.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
           ?: BigDecimal.ZERO
}

private fun List<KontantstøttePeriodeDto>.finnKontantstøtteBeløp(utgiftsMåned: UtgiftsMåned): BigDecimal {
    return this.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
           ?: BigDecimal.ZERO
}