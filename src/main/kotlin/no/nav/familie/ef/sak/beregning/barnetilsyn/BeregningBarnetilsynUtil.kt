package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.felles.dto.Periode
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.time.LocalDate
import java.time.YearMonth

data class MaxbeløpBarnetilsynSats(val fraOgMedDato: LocalDate,
                                   val tilOgMedDato: LocalDate,
                                   val maxbeløp: Map<Int, Int>)

object BeregningBarnetilsynUtil {

    val satserForBarnetilsyn: List<MaxbeløpBarnetilsynSats> =
            listOf(MaxbeløpBarnetilsynSats(fraOgMedDato = LocalDate.of(2022, 1, 1),
                                           tilOgMedDato = LocalDate.MAX,
                                           maxbeløp = mapOf(1 to 4250, 2 to 5545, 3 to 6284)),
                   MaxbeløpBarnetilsynSats(fraOgMedDato = LocalDate.of(2021, 1, 1),
                                           tilOgMedDato = YearMonth.of(2021, 12).atEndOfMonth(),
                                           maxbeløp = mapOf(1 to 4195, 2 to 5474, 3 to 6203)),
                   MaxbeløpBarnetilsynSats(fraOgMedDato = LocalDate.of(2020, 1, 1),
                                           tilOgMedDato = YearMonth.of(2020, 12).atEndOfMonth(),
                                           maxbeløp = mapOf(1 to 4053, 2 to 5289, 3 to 5993))
            )

    fun lagBeløpsPeriodeBarnetilsyn(utgiftsperiode: UtgiftsMåned,
                                    kontantstøtteBeløp: BigDecimal,
                                    tilleggsstønadBeløp: BigDecimal,
                                    antallBarnIPeriode: Int): BeløpsperiodeBarnetilsynDto {
        val beløpPeriode: BigDecimal =
                beregnPeriodeBeløp(utgiftsperiode.utgifter,
                                   kontantstøtteBeløp,
                                   tilleggsstønadBeløp,
                                   antallBarnIPeriode,
                                   utgiftsperiode.årMåned)

        return BeløpsperiodeBarnetilsynDto(utgiftsperiode.årMåned.tilPeriode(),
                                           beløpPeriode,
                                           BeregningsgrunnlagBarnetilsynDto(
                                                   utgifter = utgiftsperiode.utgifter,
                                                   kontantstøttebeløp = kontantstøtteBeløp,
                                                   tilleggsstønadsbeløp = tilleggsstønadBeløp,
                                                   antallBarn = antallBarnIPeriode))
    }

    fun beregnPeriodeBeløp(periodeutgift: BigDecimal,
                           kontantstøtteBeløp: BigDecimal,
                           tillegsønadBeløp: BigDecimal,
                           antallBarn: Int,
                           årMåned: YearMonth) =
            maxOf(ZERO, minOf(((periodeutgift - kontantstøtteBeløp).multiply(0.64.toBigDecimal()) ) - tillegsønadBeløp,
                              satserForBarnetilsyn.hentSatsFor(antallBarn, årMåned).toBigDecimal()))

    private fun YearMonth.tilPeriode(): Periode {
        return Periode(this.atDay(1),
                       this.atEndOfMonth())
    }
}

fun List<MaxbeløpBarnetilsynSats>.hentSatsFor(antallBarn: Int, årMåned: YearMonth): Int {
    if(antallBarn==0){
        return 0
    }
    val maxbeløpBarnetilsynSats = this.singleOrNull {
        it.fraOgMedDato <= årMåned.atDay(1) && it.tilOgMedDato >= årMåned.atDay(1)
    } ?: error("Kunne ikke finne barnetilsyn sats for dato: $årMåned ")

    return maxbeløpBarnetilsynSats.maxbeløp[minOf(antallBarn, 3)]
           ?: error { "Kunne ikke finne barnetilsyn sats for antallBarn: $antallBarn periode: $årMåned " }
}