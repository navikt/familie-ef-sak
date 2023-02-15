package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class MaxbeløpBarnetilsynSats(
    val periode: Datoperiode,
    val maxbeløp: Map<Int, Int>,
)

object BeregningBarnetilsynUtil {

    private val eldreBarnetilsynsatser: List<MaxbeløpBarnetilsynSats> =
        listOf(
            MaxbeløpBarnetilsynSats(
                Datoperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12)),
                maxbeløp = mapOf(1 to 4250, 2 to 5545, 3 to 6284),
            ),
            MaxbeløpBarnetilsynSats(
                Datoperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 12)),
                maxbeløp = mapOf(1 to 4195, 2 to 5474, 3 to 6203),
            ),
            MaxbeløpBarnetilsynSats(
                Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 12)),
                maxbeløp = mapOf(1 to 4053, 2 to 5289, 3 to 5993),
            ),
        )

    val satserForBarnetilsyn: List<MaxbeløpBarnetilsynSats> =
        listOf(
            MaxbeløpBarnetilsynSats(
                Datoperiode(LocalDate.of(2023, 1, 1), LocalDate.MAX),
                maxbeløp = mapOf(1 to 4369, 2 to 5700, 3 to 6460),
            ),
        ) + eldreBarnetilsynsatser

    val ikkeGjeldendeSatserForBarnetilsyn: List<MaxbeløpBarnetilsynSats> =
        listOf(
            MaxbeløpBarnetilsynSats(
                Datoperiode(LocalDate.of(2022, 1, 1), LocalDate.MAX),
                maxbeløp = mapOf(1 to 4250, 2 to 5545, 3 to 6284),
            ),
        ) + eldreBarnetilsynsatser.filter { !it.periode.inneholder(LocalDate.of(2022, 1, 1)) }

    fun lagBeløpsPeriodeBarnetilsyn(
        utgiftsperiode: UtgiftsMåned,
        kontantstøtteBeløp: BigDecimal,
        tilleggsstønadBeløp: BigDecimal,
        barn: List<UUID>,
        brukIkkeVedtatteSatser: Boolean,
    ): BeløpsperiodeBarnetilsynDto {
        val beregnedeBeløp: BeregnedeBeløp =
            beregnPeriodeBeløp(
                utgiftsperiode.utgifter,
                kontantstøtteBeløp,
                tilleggsstønadBeløp,
                barn.size,
                utgiftsperiode.årMåned,
                brukIkkeVedtatteSatser,
            )

        return BeløpsperiodeBarnetilsynDto(
            periode = Månedsperiode(utgiftsperiode.årMåned),
            beløp = beregnedeBeløp.utbetaltBeløp.roundUp().toInt(),
            beløpFørFratrekkOgSatsjustering = beregnedeBeløp.beløpFørFratrekkOgSatsjustering.roundUp().toInt(),
            sats = beregnedeBeløp.makssats,
            beregningsgrunnlag = BeregningsgrunnlagBarnetilsynDto(
                utgifter = utgiftsperiode.utgifter,
                kontantstøttebeløp = kontantstøtteBeløp,
                tilleggsstønadsbeløp = tilleggsstønadBeløp,
                antallBarn = barn.size,
                barn = barn,
            ),
            aktivitetstype = utgiftsperiode.aktivitetstype,
            periodetype = utgiftsperiode.periodetype,
        )
    }

    data class BeregnedeBeløp(val utbetaltBeløp: BigDecimal, val beløpFørFratrekkOgSatsjustering: BigDecimal, val makssats: Int)

    fun beregnPeriodeBeløp(
        periodeutgift: BigDecimal,
        kontantstøtteBeløp: BigDecimal,
        tilleggsstønadBeløp: BigDecimal,
        antallBarn: Int,
        årMåned: YearMonth,
        brukIkkeVedtatteSatser: Boolean,
    ): BeregnedeBeløp {
        val beløpFørFratrekkOgSatsjustering =
            kalkulerUtbetalingsbeløpFørFratrekkOgSatsjustering(periodeutgift, kontantstøtteBeløp)

        val maxSatsBeløp = when (brukIkkeVedtatteSatser) {
            true -> ikkeGjeldendeSatserForBarnetilsyn.hentSatsFor(antallBarn, årMåned).toBigDecimal()
            false -> satserForBarnetilsyn.hentSatsFor(antallBarn, årMåned).toBigDecimal()
        }

        val beløpFørFratrekk = minOf(beløpFørFratrekkOgSatsjustering, maxSatsBeløp)
        val utbetaltBeløp = beløpFørFratrekk - tilleggsstønadBeløp

        return BeregnedeBeløp(
            utbetaltBeløp = maxOf(ZERO, utbetaltBeløp),
            beløpFørFratrekkOgSatsjustering = beløpFørFratrekkOgSatsjustering,
            maxSatsBeløp.toInt(),
        )
    }

    fun kalkulerUtbetalingsbeløpFørFratrekkOgSatsjustering(
        periodeutgift: BigDecimal,
        kontantstøtteBeløp: BigDecimal,
    ) =
        maxOf(ZERO, (periodeutgift - kontantstøtteBeløp).multiply(0.64.toBigDecimal()))
}

fun BigDecimal.roundUp(): BigDecimal = this.setScale(0, RoundingMode.UP)

fun List<MaxbeløpBarnetilsynSats>.hentSatsFor(antallBarn: Int, årMåned: YearMonth): Int {
    if (antallBarn == 0) {
        return 0
    }
    val maxbeløpBarnetilsynSats = this.singleOrNull {
        it.periode.inneholder(årMåned.atDay(1))
    } ?: error("Kunne ikke finne barnetilsyn sats for dato: $årMåned ")

    return maxbeløpBarnetilsynSats.maxbeløp[minOf(antallBarn, 3)]
        ?: error { "Kunne ikke finne barnetilsyn sats for antallBarn: $antallBarn periode: $årMåned " }
}
