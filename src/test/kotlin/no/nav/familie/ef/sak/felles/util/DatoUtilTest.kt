package no.nav.familie.ef.sak.felles.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DatoUtilTest {

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