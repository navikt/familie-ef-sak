package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.kontrakter.ef.søknad.Adresse
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SøknadsskjemaMapperTest {

    @Test
    internal fun `skal mappe søknad som mangler datoer for stønadsstart`() {
        val stønadsstart = Søknadsfelt(
            "Stønadsstart",
            Stønadsstart(
                null,
                null,
                Søknadsfelt(
                    "Søker du stønad fra et bestemt tidspunkt",
                    false
                )
            )
        )
        val kontraktsøknad = Testsøknad.søknadOvergangsstønad.copy(stønadsstart = stønadsstart)
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(kontraktsøknad)
        assertThat(søknadTilLagring.søkerFraBestemtMåned).isEqualTo(false)
        assertThat(søknadTilLagring.søkerFra).isNull()
    }

    @Test
    internal fun `skal mappe samboer fødselsdato`() {
        val nyPerson = TestsøknadBuilder.Builder().defaultPersonMinimum(fødselsdato = LocalDate.now())
        val søknad =
            TestsøknadBuilder.Builder().setBosituasjon(samboerdetaljer = nyPerson).build().søknadOvergangsstønad

        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
        assertThat(søknadTilLagring.bosituasjon.samboer!!.fødselsdato!!).isEqualTo(LocalDate.now())
    }

    @Test
    internal fun `skal mappe feltet skalBoHosSøker`() {
        val svarSkalBarnetBoHosSøker = "jaMenSamarbeiderIkke"
        val barn = TestsøknadBuilder.Builder()
            .defaultBarn()
            .copy(
                skalBarnetBoHosSøker = Søknadsfelt("", "", null, svarSkalBarnetBoHosSøker)
            )
        val søknad = TestsøknadBuilder.Builder()
            .build().søknadOvergangsstønad.copy(barn = Søknadsfelt("", listOf(barn)))

        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
        assertThat(søknadTilLagring.barn.first().skalBoHosSøker).isEqualTo(svarSkalBarnetBoHosSøker)
    }

    @Nested
    inner class AdresseTilAdresseopplysninger {
        @Test
        internal fun `skal mappe adress fra personalia til opplysninger om adresse då det er den som vises til brukeren`() {
            val adresse = Adresse(
                adresse = "adresse",
                postnummer = "1234",
                poststedsnavn = "Sted",
                land = null
            )
            val søknad = TestsøknadBuilder.Builder().setPersonalia(adresse = adresse).build().søknadOvergangsstønad
            assertThat(SøknadsskjemaMapper.tilDomene(søknad).adresseopplysninger?.adresse).isEqualTo("adresse, 1234 Sted")
        }

        @Test
        internal fun `poststed skal ikke vises hvis det ikke er med`() {
            val adresse = Adresse(
                adresse = "adresse",
                postnummer = "1234",
                poststedsnavn = null,
                land = "Land"
            )
            val søknad = TestsøknadBuilder.Builder().setPersonalia(adresse = adresse).build().søknadOvergangsstønad
            assertThat(SøknadsskjemaMapper.tilDomene(søknad).adresseopplysninger?.adresse).isEqualTo("adresse, 1234, Land")
        }
    }
}
