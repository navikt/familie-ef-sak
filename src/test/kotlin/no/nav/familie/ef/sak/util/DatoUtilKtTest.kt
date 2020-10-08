package no.nav.familie.ef.sak.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
}