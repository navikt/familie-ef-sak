package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeregningsgrunnlagBarnetilsynDto
import no.nav.familie.ef.sak.felles.dto.Periode
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDate

object BeregningBarnetilsynUtil {

    fun lagBeløpsPeriodeBarnetilsyn(utgiftsperiode: UtgiftsperiodeDto,
                                    kontrantstøtteBeløp: BigDecimal,
                                    tillegsønadBeløp: BigDecimal,
                                    antallBarnIPeriode: Int,
                                    periodeDato: LocalDate): BeløpsperiodeBarnetilsynDto {
        val beløpPeriode1: BigDecimal =
                beregnPeriodeBeløp(utgiftsperiode.utgifter,
                                   kontrantstøtteBeløp,
                                   tillegsønadBeløp,
                                   antallBarnIPeriode,
                                   periodeDato)

        return BeløpsperiodeBarnetilsynDto(Periode(utgiftsperiode.årMånedFra.atDay(1),
                                                   utgiftsperiode.årMånedTil.atEndOfMonth()),
                                           beløpPeriode1,
                                           BeregningsgrunnlagBarnetilsynDto(
                                                   utgiftsbeløp = ZERO,
                                                   kontantstøttebeløp = ZERO,
                                                   tilleggsstønadsbeløp = ZERO,
                                                   antallBarn = antallBarnIPeriode))
    }

    fun beregnPeriodeBeløp(periodeutgift: BigDecimal,
                           kontrantstøtteBeløp: BigDecimal,
                           tillegsønadBeløp: BigDecimal,
                           antallBarn: Int,
                           periodeDato: LocalDate) =
            minOf(((periodeutgift - kontrantstøtteBeløp) * 0.64.toBigDecimal()) - tillegsønadBeløp,
                  satserForBarnetilsyn.hentSatsFor(antallBarn, periodeDato).toBigDecimal())

}

private fun List<MaxbeløpBarnetilsynSats>.hentSatsFor(antallBarn: Int, årMåned: LocalDate): Int {
    val maxbeløpBarnetilsynSats = this.filter {
        it.fraOgMedDato <= årMåned && it.tilOgMedDato >= årMåned
    }.singleOrNull() ?: error("Kunne ikke finne barnetilsyn sats for dato: $årMåned ")

    return maxbeløpBarnetilsynSats.maxbeløp[minOf(antallBarn, 3)]
           ?: error { "Kunne ikke finne barnetilsyn sats for antallBarn: $antallBarn periode: $årMåned " }
}