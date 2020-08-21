package no.nav.familie.ef.sak.no.nav.familie.ef.sak.mapper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MapperTestUtilTest {
    data class Foo(val navn: String? = "foo", val dato: LocalDate? = LocalDate.now(), val list: List<Bar>? = arrayListOf(Bar()))
    data class Bar(val navn: String? = "bar")

    @Test
    internal fun `alle verdier er satt`() {
        sjekkAtAlleVerdierErSatt(Foo())
    }

    @Test
    internal fun `sjekker at felter ikke er satt`() {
        assertThat(Assertions.catchThrowable { sjekkAtAlleVerdierErSatt(Foo(navn = null)) })
                .hasMessage(forventetFeilmelding("Foo", "navn"))

        assertThat(Assertions.catchThrowable { sjekkAtAlleVerdierErSatt(Foo(dato = null)) })
                .hasMessage(forventetFeilmelding("Foo", "dato"))

        assertThat(Assertions.catchThrowable { sjekkAtAlleVerdierErSatt(Foo(list = null)) })
                .hasMessage(forventetFeilmelding("Foo", "list"))

        assertThat(Assertions.catchThrowable { sjekkAtAlleVerdierErSatt(Foo(list = listOf())) })
                .hasMessage(forventetFeilmeldingTomListe("Foo", "list"))

        assertThat(Assertions.catchThrowable { sjekkAtAlleVerdierErSatt(Foo(list = listOf(Bar(navn = null)))) })
                .hasMessage(forventetFeilmelding("Bar", "navn"))
    }

    private fun forventetFeilmeldingTomListe(klassenavn: String, feltnavn: String) = "$klassenavn har en tom liste i felt $feltnavn"
    private fun forventetFeilmelding(klassenavn: String, feltnavn: String) = "$klassenavn har ingen verdi i felt $feltnavn"
}
