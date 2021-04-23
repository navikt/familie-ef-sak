package no.nav.familie.ef.sak.api.beregning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal
import java.time.LocalDate

internal class BeregningTest {

    @TestFactory
    fun `skal finne grunnbeløp mellom perioder`(): List<DynamicTest> {
        val testData = listOf(
                Pair("2000-01-01", "2005-01-01") to listOf(Triple("2000-01-01", "2000-05-01", 46950),
                                                           Triple("2000-05-01", "2001-05-01", 49090),
                                                           Triple("2001-05-01", "2002-05-01", 51360),
                                                           Triple("2002-05-01", "2003-05-01", 54170),
                                                           Triple("2003-05-01", "2004-05-01", 56861),
                                                           Triple("2004-05-01", "2005-01-01", 58778)),

                Pair("2020-04-30", "2020-05-01") to listOf(Triple("2020-04-30", "2020-05-01", 99858),
                                                           Triple("2020-05-01", "2020-05-01", 101351)),

                Pair("2019-05-01", "2020-05-01") to listOf(Triple("2019-05-01", "2020-05-01", 99858),
                                                           Triple("2020-05-01", "2020-05-01", 101351)),

                Pair("2019-05-01", "2020-04-30") to listOf(Triple("2019-05-01", "2020-04-30", 99858)),

                Pair("2021-01-01", "2021-03-01") to listOf(Triple("2021-01-01", "2021-03-01", 101351))

        )
        return testData
                .map { (periode, fasit) ->
                    dynamicTest("skal finne grunnbeløp for perioden=[${periode.first}-${periode.second}] med forventet svar: $fasit") {
                        assertThat(finnGrunnbeløpsPerioder(LocalDate.parse(periode.first),
                                                                      LocalDate.parse(periode.second)))
                                .isEqualTo(fasit.map {
                                    Beløpsperiode(LocalDate.parse(it.first),
                                                  LocalDate.parse(it.second),
                                                null,
                                                  it.third.toBigDecimal(),
                                                  it.third.toBigDecimal())
                                })
                    }
                }
    }

}