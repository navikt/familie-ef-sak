package no.nav.familie.ef.sak.integration.dto.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PdlPersonUtilTest {

    @Test
    internal fun `skal formetere navn med og uten mellomnavn`() {
        val navn = Navn("fornavn", null, "etternavn", metadata = Metadata(endringer = emptyList()))
        assertThat(navn.visningsnavn())
                .isEqualTo("fornavn etternavn")

        assertThat(navn.copy(mellomnavn = "mellomnavn").visningsnavn())
                .isEqualTo("fornavn mellomnavn etternavn")
    }

    @Test
    internal fun `gjeldende navn skal hente navnet som ble sist registrert`() {
        val gjeldendeNavn = listOf(Navn("a", null, "a", metadata = metadata(LocalDate.of(2020, 1, 15))),
                                   Navn("b", null, "b", metadata = metadata(LocalDate.of(2020, 1, 17))),
                                   Navn("c", null, "c", metadata = metadata(LocalDate.of(2020, 1, 16))))
                .gjeldende().fornavn
        assertThat(gjeldendeNavn).isEqualTo("b")
    }

    private fun metadata(registrertDato: LocalDate) = Metadata(endringer = listOf(MetadataEndringer(registrertDato)))
}
