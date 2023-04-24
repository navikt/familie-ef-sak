package no.nav.familie.ef.sak.felles.util

import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

internal class DatoUtilTest {

    @Nested
    inner class DatoEllerIdag {

        @Test
        internal fun `Hvis localDate er null skal den returnere dagens dato`() {
            assertThat(datoEllerIdag(null)).isEqualTo(LocalDate.now())
        }

        @Test
        internal fun `Hvis localDate ikke er null skal den returnere datoen`() {
            val dato = LocalDate.of(2020, 1, 1)
            assertThat(datoEllerIdag(dato)).isEqualTo(dato)
        }
    }

    @Nested
    inner class IsEqualOrBefore {

        @Test
        internal fun `localdate - isEqualOrBefore`() {
            val første = LocalDate.now().minusDays(1)
            val siste = LocalDate.now()
            assertThat(første.isEqualOrBefore(første)).isTrue
            assertThat(første.isEqualOrBefore(siste)).isTrue
            assertThat(siste.isEqualOrBefore(første)).isFalse
        }

        @Test
        internal fun `første dato av 2 datoer`() {
            val første = LocalDate.now().minusDays(1)
            val siste = LocalDate.now()
            assertThat(første.isEqualOrAfter(første)).isTrue
            assertThat(første.isEqualOrAfter(siste)).isFalse
            assertThat(siste.isEqualOrAfter(første)).isTrue
        }
    }

    @Nested
    inner class ErPåfølgende {

        @Test
        internal fun `er påfølgende måned`() {
            val gjeldende = YearMonth.of(2020, 12)
            val påfølgende = YearMonth.of(2021, 1)
            val ikkePåfølgende = YearMonth.of(2021, 2)
            assertThat(gjeldende.erPåfølgende(påfølgende)).isTrue
            assertThat(gjeldende.erPåfølgende(ikkePåfølgende)).isFalse
        }
    }

    @Nested
    inner class Min {

        @Test
        internal fun `min av LocalDate`() {
            val first = LocalDate.of(2021, 1, 1)
            val second = LocalDate.of(2021, 2, 1)
            assertThat(min(null as LocalDate?, null as LocalDate?)).isNull()
            assertThat(min(first, null)).isEqualTo(first)
            assertThat(min(null, first)).isEqualTo(first)
            assertThat(min(first, first)).isEqualTo(first)
            assertThat(min(first, second)).isEqualTo(first)
            assertThat(min(second, first)).isEqualTo(first)
        }

        @Test
        internal fun `min av LocalDateTime`() {
            val first = LocalDate.of(2021, 1, 1).atStartOfDay()
            val second = LocalDate.of(2021, 2, 1).atStartOfDay()
            assertThat(min(null as LocalDateTime?, null as LocalDateTime?)).isNull()
            assertThat(min(first, null)).isEqualTo(first)
            assertThat(min(null, first)).isEqualTo(first)
            assertThat(min(first, first)).isEqualTo(first)
            assertThat(min(first, second)).isEqualTo(first)
            assertThat(min(second, first)).isEqualTo(first)
        }
    }

    @Nested
    inner class SkoleårTest {

        private val skoleår2021 = Skoleår(Year.of(2021))

        @Test
        internal fun `skal mappe alle måneder fra juli til samme år`() {
            val år = 2021
            IntRange(7, 12).map { fraMåned ->
                val fra = YearMonth.of(år, fraMåned)
                IntRange(fraMåned, 12).forEach { tilMåned ->
                    assertThat(Skoleår(Månedsperiode(fra, YearMonth.of(år, tilMåned)))).isEqualTo(skoleår2021)
                }
                IntRange(1, 6).forEach { tilMåned ->
                    assertThat(Skoleår(Månedsperiode(fra, YearMonth.of(år + 1, tilMåned)))).isEqualTo(skoleår2021)
                }
            }
        }

        @Test
        internal fun `skal mappe alle måneder fra januar til juni til forrige år`() {
            val år = 2022
            IntRange(1, 6).map { fraMåned ->
                val fra = YearMonth.of(år, fraMåned)
                IntRange(fraMåned, 8).forEach { tilMåned ->
                    assertThat(Skoleår(Månedsperiode(fra, YearMonth.of(år, tilMåned)))).isEqualTo(skoleår2021)
                }
            }
        }

        @Test
        internal fun `kan ikke ha tildato før fradato`() {
            assertThatThrownBy {
                val fra = YearMonth.of(2021, 7)
                Skoleår(Månedsperiode(fra, fra.minusMonths(1)))
            }.hasMessage("Til-og-med før fra-og-med: 2021-07 > 2021-06")
        }

        @Test
        internal fun `fradato etter juni må ha tildato før september`() {
            val feilmelding = "Ugyldig skoleårsperiode: Når tildato er i neste år, så må måneden være før september"
            assertThatThrownBy { Skoleår(Månedsperiode(YearMonth.of(2021, 7), YearMonth.of(2022, 9))) }
                .hasMessage(feilmelding)
            assertThatThrownBy { Skoleår(Månedsperiode(YearMonth.of(2021, 7), YearMonth.of(2022, 12))) }
                .hasMessage(feilmelding)
        }

        @Test
        internal fun `fradato etter juni må ha tildato i neste år`() {
            assertThatThrownBy { Skoleår(Månedsperiode(YearMonth.of(2021, 8), YearMonth.of(2023, 1))) }
                .hasMessage("Ugyldig skoleårsperiode: Fradato og tildato må være i det samme skoleåret")
        }

        @Test
        internal fun `fradato før juli må ha sluttmåned før september`() {
            val feilmelding = "Ugyldig skoleårsperiode: Fradato før juli må ha sluttmåned før september"
            assertThatThrownBy { Skoleår(Månedsperiode(YearMonth.of(2021, 6), YearMonth.of(2021, 9))) }
                .hasMessage(feilmelding)
            assertThatThrownBy { Skoleår(Månedsperiode(YearMonth.of(2021, 6), YearMonth.of(2021, 12))) }
                .hasMessage(feilmelding)
        }

        @Test
        internal fun `fradato før juli med må ha tildato i samme år`() {
            val feilmelding = "Ugyldig skoleårsperiode: Fradato før juli må ha tildato i det samme året"
            assertThatThrownBy { Skoleår(Månedsperiode(YearMonth.of(2021, 6), YearMonth.of(2022, 1))) }
                .hasMessage(feilmelding)
            assertThatThrownBy { Skoleår(Månedsperiode(YearMonth.of(2021, 6), YearMonth.of(2022, 2))) }
                .hasMessage(feilmelding)
        }

        @Test
        internal fun `formater skoleår viser siste 2 sifrene i skoleåret`() {
            assertThat(Skoleår(Year.of(2021)).toString()).isEqualTo("21/22")
            assertThat(Skoleår(Year.of(1999)).toString()).isEqualTo("99/00")
        }
    }

    @Nested
    inner class HarGåttAntallTimer {

        @Test
        internal fun `sjekker har gått mer enn X timer`() {
            assertThat(LocalDateTime.now().harGåttAntallTimer(4)).isFalse
            assertThat(LocalDateTime.now().minusHours(3).harGåttAntallTimer(4)).isFalse
            assertThat(LocalDateTime.now().minusHours(5).harGåttAntallTimer(4)).isTrue
        }
    }
}
