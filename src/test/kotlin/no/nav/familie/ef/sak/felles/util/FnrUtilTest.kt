package no.nav.familie.ef.sak.felles.util

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class FnrUtilTest {

    @Test
    internal fun `skal feile hvis fnr ikke inneholder 11 tegn`() {
        listOf("1", "1234567890", "123456789012").forEach {
            assertThatThrownBy { FnrUtil.validerIdent(it) }
                    .hasMessageContaining("Ugyldig personident. Det må være 11 sifre")
        }
    }

    @Test
    internal fun `skal feile hvis fnr ikke inneholder 11 sifre`() {
        assertThatThrownBy { FnrUtil.validerIdent("1234567890b") }
                .hasMessageContaining("Ugyldig personident. Det kan kun inneholde tall")
    }

    @Test
    internal fun `fnr med 11 siffer er gyldig`() {
        FnrUtil.validerIdent("12345678901")
    }
}