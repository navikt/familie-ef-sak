package no.nav.familie.ef.sak.testutil

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.beregning.Grunnbeløp
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDate
import java.time.YearMonth


fun mockTestMedGrunnbeløpFra( grunnbeløp2023: Grunnbeløp,test: () -> Unit) {

    val indeks2023 =
        Grunnbeløpsperioder.grunnbeløpsperioder.indexOfFirst { it.periode.fom == YearMonth.of(2023, 5) }
    val grunnbeløpFør2023 =
        Grunnbeløpsperioder.grunnbeløpsperioder.slice(indeks2023 until Grunnbeløpsperioder.grunnbeløpsperioder.size)

    mockkObject(Grunnbeløpsperioder)
    every { Grunnbeløpsperioder.grunnbeløpsperioder } returns listOf(grunnbeløp2023) + grunnbeløpFør2023
    every { Grunnbeløpsperioder.nyesteGrunnbeløp } returns grunnbeløp2023
    every { Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed } returns YearMonth.of(2023, 5)

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
