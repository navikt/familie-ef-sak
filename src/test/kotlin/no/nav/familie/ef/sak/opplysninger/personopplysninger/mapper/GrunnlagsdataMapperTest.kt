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
                fødselsår = fødselsdato.fødselsår,
                fødselsdato = fødselsdato.fødselsdato,
                metadata = fødselsdato.metadata,
                fødested = fødested.fødested,
                fødekommune = fødested.fødekommune,
                fødeland = fødested.fødeland,
            )

        val annenFødsel =
            Fødsel(
                fødselsår = annenFødselsdato.fødselsår,
                fødselsdato = annenFødselsdato.fødselsdato,
                metadata = annenFødselsdato.metadata,
                fødested = annetFødested.fødested,
                fødekommune = annetFødested.fødekommune,
                fødeland = annetFødested.fødeland,
            )

        val zippedeFødsler = GrunnlagsdataMapper.mapFødsler(listOf(fødselsdato, annenFødselsdato), listOf(fødested, annetFødested))

        assertThat(zippedeFødsler).contains(fødsel)
        assertThat(zippedeFødsler).contains(annenFødsel)
    }
}
