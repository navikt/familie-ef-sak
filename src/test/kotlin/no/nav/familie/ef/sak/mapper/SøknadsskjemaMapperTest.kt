package no.nav.familie.ef.sak.mapper

import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

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
}