package no.nav.familie.ef.sak.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class DatoUtilKtTest {

    @Test
    internal fun `min skal håndtere minste dato og håndtere null`() {
        assertThat(min(LocalDateTime.MIN, LocalDateTime.MAX))
                .isEqualTo(LocalDateTime.MIN)

        assertThat(min(LocalDateTime.MAX, LocalDateTime.MIN))
                .isEqualTo(LocalDateTime.MIN)

        assertThat(min(null, LocalDateTime.MIN))
                .isEqualTo(LocalDateTime.MIN)
        assertThat(min(LocalDateTime.MIN, null))
                .isEqualTo(LocalDateTime.MIN)

        assertThat(min(null, null))
                .isNull()
    }

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