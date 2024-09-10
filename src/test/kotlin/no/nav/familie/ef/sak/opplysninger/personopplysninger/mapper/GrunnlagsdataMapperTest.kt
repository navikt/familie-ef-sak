package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødested
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødselsdato
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.LocalDate

internal class GrunnlagsdataMapperTest {
    @Test
    fun `to forskjellige fødselsdatoer og fødesteder skal bli merget til to Fødsel-objekter`() {
        val fødselsdato = fødselsdato(2020, LocalDate.of(2020, 1, 1))
        val fødested = fødested("land", "sted", "kommune")

        val annenFødselsdato = fødselsdato(2020, LocalDate.of(2022, 1, 1))
        val annetFødested = fødested("annetLand", "annetSted", "annenKommune")

        val fødsel =
            Fødsel(
                foedselsaar = fødselsdato.fødselsår,
                foedselsdato = fødselsdato.fødselsdato,
                foedested = fødested.fødested,
                foedekommune = fødested.fødekommune,
                foedeland = fødested.fødeland,
            )

        val annenFødsel =
            Fødsel(
                foedselsaar = annenFødselsdato.fødselsår,
                foedselsdato = annenFødselsdato.fødselsdato,
                foedested = annetFødested.fødested,
                foedekommune = annetFødested.fødekommune,
                foedeland = annetFødested.fødeland,
            )

        val zippedeFødsler = GrunnlagsdataMapper.mapFødsler(listOf(fødselsdato, annenFødselsdato), listOf(fødested, annetFødested))

        assertThat(zippedeFødsler).contains(fødsel)
        assertThat(zippedeFødsler).contains(annenFødsel)
    }
}
