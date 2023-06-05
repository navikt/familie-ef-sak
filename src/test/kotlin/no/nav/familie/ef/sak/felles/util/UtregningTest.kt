package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.felles.util.Utregning.rundNedTilNærmeste100
import no.nav.familie.ef.sak.felles.util.Utregning.rundNedTilNærmeste1000
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class UtregningTest {

    @Test
    internal fun `skal avrunde ned til næmeste 100`() {
        assertThat(rundNedTilNærmeste100(BigDecimal(0))).isEqualTo(0.toBigDecimal())
        assertThat(rundNedTilNærmeste100(BigDecimal(50))).isEqualTo(0.toBigDecimal())
        assertThat(rundNedTilNærmeste100(BigDecimal(99.999))).isEqualTo(0.toBigDecimal())
        assertThat(rundNedTilNærmeste100(BigDecimal(100))).isEqualTo(100.toBigDecimal())
        assertThat(rundNedTilNærmeste100(BigDecimal(101))).isEqualTo(100.toBigDecimal())
        assertThat(rundNedTilNærmeste100(BigDecimal(199))).isEqualTo(100.toBigDecimal())
        assertThat(rundNedTilNærmeste100(BigDecimal(2199))).isEqualTo(2100.toBigDecimal())
    }

    @Test
    internal fun `skal avrunde ned til næmeste 1000`() {
        assertThat(rundNedTilNærmeste1000(BigDecimal(0))).isEqualTo(0)
        assertThat(rundNedTilNærmeste1000(BigDecimal(50))).isEqualTo(0)
        assertThat(rundNedTilNærmeste1000(BigDecimal(999.999))).isEqualTo(0)
        assertThat(rundNedTilNærmeste1000(BigDecimal(1000))).isEqualTo(1000)
        assertThat(rundNedTilNærmeste1000(BigDecimal(1001))).isEqualTo(1000)
        assertThat(rundNedTilNærmeste1000(BigDecimal(1999))).isEqualTo(1000)
        assertThat(rundNedTilNærmeste1000(BigDecimal(2199))).isEqualTo(2000)
    }
}
