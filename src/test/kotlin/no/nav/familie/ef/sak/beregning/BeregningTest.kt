package no.nav.familie.ef.sak.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.time.LocalDate

internal class BeregningTest {
    @TestFactory
    fun `skal finne grunnbeløp mellom perioder`(): List<DynamicTest> {
        val testData =
            listOf(
                Pair("2000-01-01", "2005-01-01") to
                    listOf(
                        Triple("2000-01-01", "2000-04-30", 46950),
                        Triple("2000-05-01", "2001-04-30", 49090),
                        Triple("2001-05-01", "2002-04-30", 51360),
                        Triple("2002-05-01", "2003-04-30", 54170),
                        Triple("2003-05-01", "2004-04-30", 56861),
                        Triple("2004-05-01", "2005-01-01", 58778),
                    ),
                Pair("2020-04-30", "2020-05-01") to
                    listOf(
                        Triple("2020-04-30", "2020-04-30", 99858),
                        Triple("2020-05-01", "2020-05-01", 101351),
                    ),
                Pair("2019-05-01", "2020-05-01") to
                    listOf(
                        Triple("2019-05-01", "2020-04-30", 99858),
                        Triple("2020-05-01", "2020-05-01", 101351),
                    ),
                Pair("2019-05-01", "2020-04-30") to listOf(Triple("2019-05-01", "2020-04-30", 99858)),
                Pair("2021-01-01", "2021-03-01") to listOf(Triple("2021-01-01", "2021-03-01", 101351)),
            )
        return testData
            .map { (periode, fasit) ->
                dynamicTest(
                    "skal finne grunnbeløp for perioden=[${periode.first}-${periode.second}] " +
                        "med forventet svar: $fasit",
                ) {
                    assertThat(
                        finnGrunnbeløpsPerioder(
                            Månedsperiode(
                                LocalDate.parse(periode.first),
                                LocalDate.parse(periode.second),
                            ),
                        ),
                    ).isEqualTo(
                        fasit.map {
                            Beløpsperiode(
                                Månedsperiode(
                                    LocalDate.parse(it.first),
                                    LocalDate.parse(it.second),
                                ),
                                beregningsgrunnlag = null,
                                beløp = it.third.toBigDecimal(),
                                beløpFørSamordning = it.third.toBigDecimal(),
                            )
                        },
                    )
                }
            }
    }
}
