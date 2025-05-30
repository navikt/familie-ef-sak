package no.nav.familie.ef.sak.testutil

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.beregning.Grunnbeløp
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun mockTestMedGrunnbeløpFra(
    sisteGrunnbeløp: Grunnbeløp,
    test: () -> Unit,
) {
    val indeksForrigeÅr =
        Grunnbeløpsperioder.grunnbeløpsperioder.indexOfFirst { it.periode.fom == YearMonth.of(sisteGrunnbeløp.periode.fom.year - 1, 5) }
    val grunnbeløpFørNestSiste =
        Grunnbeløpsperioder.grunnbeløpsperioder.slice(indeksForrigeÅr + 1 until Grunnbeløpsperioder.grunnbeløpsperioder.size)
    val nestSistePeriodeKuttet = Grunnbeløpsperioder.grunnbeløpsperioder[indeksForrigeÅr].periode.copy(tom = sisteGrunnbeløp.periode.fom.minusMonths(1))
    val nestSisteGrunnbeløp = Grunnbeløpsperioder.grunnbeløpsperioder[indeksForrigeÅr].copy(periode = nestSistePeriodeKuttet)
    mockkObject(Grunnbeløpsperioder)
    every { Grunnbeløpsperioder.grunnbeløpsperioder } returns listOf(sisteGrunnbeløp) + nestSisteGrunnbeløp + grunnbeløpFørNestSiste
    every { Grunnbeløpsperioder.nyesteGrunnbeløp } returns sisteGrunnbeløp
    every { Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed } returns sisteGrunnbeløp.periode.fom

    test()

    unmockkObject(Grunnbeløpsperioder)
}

fun mockTestMedGrunnbeløpFra2022(test: () -> Unit) {
    val grunnbeløp2022 =
        Grunnbeløp(
            periode = Månedsperiode(YearMonth.parse("2022-05"), YearMonth.from(LocalDate.MAX)),
            grunnbeløp = 111_477.toBigDecimal(),
            perMnd = 9_290.toBigDecimal(),
            gjennomsnittPerÅr = 109_784.toBigDecimal(),
        )

    mockTestMedGrunnbeløpFra(grunnbeløp2022, test)
}

fun mockTestMedGrunnbeløpFra2023(test: () -> Unit) {
    val grunnbeløp2023 =
        Grunnbeløp(
            periode = Månedsperiode(YearMonth.parse("2023-05"), YearMonth.from(LocalDate.MAX)),
            grunnbeløp = 118_620.toBigDecimal(),
            perMnd = BigDecimal.ZERO,
            gjennomsnittPerÅr = BigDecimal.ZERO,
        )

    mockTestMedGrunnbeløpFra(grunnbeløp2023, test)
}

fun mockTestMedGrunnbeløpFra2024(test: () -> Unit) {
    val grunnbeløp2024 =
        Grunnbeløp(
            periode = Månedsperiode("2024-05" to "2025-04"),
            grunnbeløp = 124_028.toBigDecimal(),
            perMnd = 10_336.toBigDecimal(),
            gjennomsnittPerÅr = 122_225.toBigDecimal(),
        )

    mockTestMedGrunnbeløpFra(grunnbeløp2024, test)
}

fun mockTestMedGrunnbeløpFra2025(test: () -> Unit) {
    val grunnbeløp2025 =
        Grunnbeløp(
            periode = Månedsperiode(YearMonth.now().minusMonths(3), YearMonth.now().plusMonths(9)),
            grunnbeløp = 130_160.toBigDecimal(),
            perMnd = 10_847.toBigDecimal(),
            gjennomsnittPerÅr = 128_116.toBigDecimal(),
        )

    mockTestMedGrunnbeløpFra(grunnbeløp2025, test)
}
