package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyndto.BeløpsperiodeBarnetilsynDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeregningBarnetilsynService {


    fun beregnYtelseBarnetilsyn(utgiftsperioder: List<UtgiftsperiodeDto>,
                                kontantstøttePerioder: List<KontantstøttePeriodeDto>,
                                tilleggsstønadsperioder: List<TilleggsstønadPeriodeDto>): List<BeløpsperiodeBarnetilsynDto> {

        // TODO valider !!!!


        // Beløp = minOf( ( (utgifter - kontantstøtte) * 0.64 ) - reduksjonsbeløp, maksBeløpAntallBarnDuHarUtgifterFor)


        val utgiftsperiode = utgiftsperioder.first()
        val size = utgiftsperiode.barn.size
        val maxbeløpBarnetilsynSats = satserForBarnetilsyn.first()
        val maxbeløpGittAntallBarn: Int = maxbeløpBarnetilsynSats.maxbeløp[size] ?: 0
        val tillegsønadBeløp = tilleggsstønadsperioder.first().beløp
        val kontrantstøtteBeløp = kontantstøttePerioder.first().beløp


        return listOf(BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(utgiftsperiode,
                                                                    kontrantstøtteBeløp,
                                                                    tillegsønadBeløp,
                                                                    maxbeløpGittAntallBarn,
                                                                    utgiftsperiode.årMånedFra.atEndOfMonth())

        )
    }
}

val satserForBarnetilsyn: List<MaxbeløpBarnetilsynSats> =
        listOf(MaxbeløpBarnetilsynSats(fraOgMedDato = LocalDate.parse("2022-01-01"),
                                       tilOgMedDato = LocalDate.MAX,
                                       maxbeløp = mapOf(1 to 4250, 2 to 5545, 3 to 6284))
        )

data class MaxbeløpBarnetilsynSats(val fraOgMedDato: LocalDate,
                                   val tilOgMedDato: LocalDate,
                                   val maxbeløp: Map<Int, Int>)