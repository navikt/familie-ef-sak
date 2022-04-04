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
        val kontantStøtteBeløp: BigDecimal =
                kontantstøttePerioder.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
                ?: BigDecimal.ZERO
        val tilleggsstønadsperiodeBeløp =
                tilleggsstønadsperioder.find { utgiftsMåned.årMåned <= it.årMånedTil && utgiftsMåned.årMåned >= it.årMånedFra }?.beløp
                ?: BigDecimal.ZERO

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

// TODO gjør et valg på implementasjon
fun List<BeløpsperiodeBarnetilsynDto>.merge2(): List<BeløpsperiodeBarnetilsynDto> {

    val sortedBy = this.sortedBy { it.periode.fradato }
    var beløpsPeriodeDto = sortedBy.first()
    var tempPeriode = beløpsPeriodeDto.periode
    val mergedeBeløpsperioder = mutableListOf<BeløpsperiodeBarnetilsynDto>()
    sortedBy.forEachIndexed { index, it ->
        if (index > 0) {
            validerSammenhengendePeriode(it, tempPeriode)
        }
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


data class Key(val beløp: Int, val grunnlag: BeregningsgrunnlagBarnetilsynDto)

fun List<BeløpsperiodeBarnetilsynDto>.merge(): List<BeløpsperiodeBarnetilsynDto> {
    val gruppert = this.groupBy { it.toKey() }
    return gruppert.entries.mapNotNull {
        val liste: List<BeløpsperiodeBarnetilsynDto> = it.value.sortedBy { it.periode.fradato }
        var akkumulatorListe = mutableListOf<BeløpsperiodeBarnetilsynDto>()
        liste.fold(akkumulatorListe) { akkumulatorListe, nestePeriodeDto ->
            val gjeldendeDto = akkumulatorListe.lastOrNull()
            if (gjeldendeDto != null && erSammenhengende(gjeldendeDto.periode, nestePeriodeDto.periode)) {
                val nyPeriode = gjeldendeDto.periode.copy(tildato = nestePeriodeDto.periode.tildato)
                akkumulatorListe.removeLast()
                akkumulatorListe.add(gjeldendeDto.copy(periode = nyPeriode))
            } else {
                akkumulatorListe.add(nestePeriodeDto)
            }
            akkumulatorListe
        }
    }.flatten()
}

private fun erSammenhengende(gjeldendePeriode: Periode,
                             nestePeriode: Periode) =
        gjeldendePeriode.tildato.month.equals(nestePeriode.fradato.minusMonths(1).month)

private fun BeløpsperiodeBarnetilsynDto.toKey() = Key(this.beløp.toInt(), this.beregningsgrunnlag)


/** TODO : Sjekk om vi skal tillate hull i perioder */
private fun validerSammenhengendePeriode(it: BeløpsperiodeBarnetilsynDto,
                                         tempPeriode: Periode) {

    if (!erSammenhengende(tempPeriode, it.periode)) {
        error("Periodene $tempPeriode og ${it.periode} er ikke sammenhengende ")
    }


}
