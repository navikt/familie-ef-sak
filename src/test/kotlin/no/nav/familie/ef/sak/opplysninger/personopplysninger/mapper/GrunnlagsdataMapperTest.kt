package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødested
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødselsdato
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnlagsdataMapperTest {
    @Test
    fun `en fødselsdato og ett fødested skal bli merget til en fødsel`() {
        val fødselsdato = fødselsdato(2020, LocalDate.of(2020, 1, 1))
        val fødested = fødested("land", "sted", "kommune")

        val fødsel =
            Fødsel(
                fødselsår = fødselsdato.fødselsår,
                fødselsdato = fødselsdato.fødselsdato,
                fødested = fødested.fødested,
                fødekommune = fødested.fødekommune,
                fødeland = fødested.fødeland,
            )

        val mappetFødsel = GrunnlagsdataMapper.mapFødsel(fødselsdato, fødested)

        assertThat(mappetFødsel).contains(fødsel)
    }

    @Test
    fun `påser at fødested blir mappet selv om fødselsår og dato mangler`() {
        val fødselsdato = fødselsdato(null, null)
        val fødested = fødested("land", "sted", "kommune")

        val fødsel =
            Fødsel(
                fødselsår = fødselsdato.fødselsår,
                fødselsdato = fødselsdato.fødselsdato,
                fødested = fødested.fødested,
                fødekommune = fødested.fødekommune,
                fødeland = fødested.fødeland,
            )

        val mappetFødsel = GrunnlagsdataMapper.mapFødsel(fødselsdato, fødested)

        assertThat(mappetFødsel).contains(fødsel)
    }
}
