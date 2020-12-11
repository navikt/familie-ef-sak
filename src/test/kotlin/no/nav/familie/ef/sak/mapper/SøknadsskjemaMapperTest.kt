package no.nav.familie.ef.sak.mapper

import no.nav.familie.kontrakter.ef.søknad.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SøknadsskjemaMapperTest {

    @Test
    internal fun `skal mappe søknad som mangler datoer for stønadsstart`() {
        val stønadsstart = Søknadsfelt("Stønadsstart", Stønadsstart(null,
                                                                    null,
                                                                    Søknadsfelt("Søker du stønad fra et bestemt tidspunkt",
                                                                                false)))
        val kontraktsøknad = Testsøknad.søknadOvergangsstønad.copy(stønadsstart = stønadsstart)
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(kontraktsøknad)
        Assertions.assertThat(søknadTilLagring.søkerFraBestemtMåned).isEqualTo(false)
        Assertions.assertThat(søknadTilLagring.søkerFra).isNull()
    }
    @Test
    internal fun `skal mappe samboer fødselsdato`() {

        val nyPerson = TestsøknadBuilder.Builder().defaultPersonMinimum(fødselsdato = LocalDate.now())
        val søknad = TestsøknadBuilder.Builder().setBosituasjon(samboerdetaljer = nyPerson).build().søknadOvergangsstønad

        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
        Assertions.assertThat(søknadTilLagring.bosituasjon.samboer!!.fødselsdato!!).isEqualTo(LocalDate.now())

    }

}