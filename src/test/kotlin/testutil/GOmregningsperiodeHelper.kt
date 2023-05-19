package no.nav.familie.ef.sak.testutil

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.beregning.Grunnbeløp
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDate
import java.time.YearMonth

fun mockTestMedGrunnbeløpFra(sisteGrunnbeløp: Grunnbeløp, test: () -> Unit) {
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
    val grunnbeløp2022 = Grunnbeløp(
        periode = Månedsperiode(YearMonth.parse("2022-05"), YearMonth.from(LocalDate.MAX)),
        grunnbeløp = 111_477.toBigDecimal(),
        perMnd = 9_290.toBigDecimal(),
        gjennomsnittPerÅr = 109_784.toBigDecimal(),
    )

    val indeks2022 =
        Grunnbeløpsperioder.grunnbeløpsperioder.indexOfFirst { it.periode.fom == YearMonth.of(2022, 5) }
    val grunnbeløpFør2022 =
        Grunnbeløpsperioder.grunnbeløpsperioder.slice(indeks2022 until Grunnbeløpsperioder.grunnbeløpsperioder.size)

    mockkObject(Grunnbeløpsperioder)
    every { Grunnbeløpsperioder.grunnbeløpsperioder } returns listOf(grunnbeløp2022) + grunnbeløpFør2022
    every { Grunnbeløpsperioder.nyesteGrunnbeløp } returns grunnbeløp2022
    every { Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed } returns YearMonth.of(2022, 5)

    test()

    unmockkObject(Grunnbeløpsperioder)
}
