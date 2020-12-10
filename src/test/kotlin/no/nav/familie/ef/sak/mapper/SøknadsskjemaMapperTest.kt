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

        //val kontraktsøknad = Testsøknad.søknadOvergangsstønad


        val nyPerson = TestsøknadBuilder.Builder().defaultPersonMinimum(fødselsdato = LocalDate.now())
        val søknad = TestsøknadBuilder.Builder().setBosituasjon(samboerdetaljer = nyPerson).build().søknadOvergangsstønad

        /**val nyFødselsdato : Søknadsfelt<LocalDate> = kontraktsøknad.bosituasjon.verdi.samboerdetaljer!!.verdi.fødselsdato!!.copy(verdi = LocalDate.now())
        val nyPersonMinimum : PersonMinimum = kontraktsøknad.bosituasjon.verdi.samboerdetaljer!!.verdi.copy(fødselsdato = nyFødselsdato)
        val nyBosituasjon : Bosituasjon = kontraktsøknad.bosituasjon.verdi.copy(samboerdetaljer = Søknadsfelt("", nyPersonMinimum))
        val nySøknad : SøknadOvergangsstønad = kontraktsøknad.copy(bosituasjon = Søknadsfelt(kontraktsøknad.bosituasjon.label, nyBosituasjon))
             **/
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
        Assertions.assertThat(søknadTilLagring.bosituasjon.samboer!!.fødselsdato!!).isEqualTo(LocalDate.now())

    }
    @Test
    internal fun `skal mappe sivilstand`(){
        val kontraktsøknad = Testsøknad.søknadOvergangsstønad
        val sivilstandsdetaljer : Sivilstandsdetaljer = kontraktsøknad.sivilstandsdetaljer.verdi
    }

}