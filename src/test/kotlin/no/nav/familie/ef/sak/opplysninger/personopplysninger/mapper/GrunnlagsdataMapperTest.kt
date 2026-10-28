package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.NAVN_MANGLER
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødested
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødselsdato
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlSøker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnlagsdataMapperTest {
    @Test
    fun `skal mappe søker uten navn og kjønn, f eks når opplysningene er annullert ved falsk identitet`() {
        val søker = GrunnlagsdataMapper.mapSøker(pdlSøker(navn = emptyList(), kjønn = null), emptyMap(), håndterFalskIdentitet = true)

        assertThat(søker.navn).isNull()
        assertThat(søker.kjønn.name).isEqualTo(Kjønn.UKJENT.name)
        assertThat(NavnDto.fraNavnEllerMangler(søker.navn).visningsnavn).isEqualTo(NAVN_MANGLER)
    }

    @Test
    fun `skal ikke tillate manglende navn når håndtering av falsk identitet er av`() {
        assertThatThrownBy { GrunnlagsdataMapper.mapSøker(pdlSøker(navn = emptyList()), emptyMap(), håndterFalskIdentitet = false) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

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
