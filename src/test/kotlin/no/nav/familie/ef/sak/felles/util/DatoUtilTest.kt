package no.nav.familie.ef.sak.felles.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

internal class DatoUtilTest {

    @Nested
    inner class datoEllerIdag {

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
    inner class erPåfølgende {

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
}