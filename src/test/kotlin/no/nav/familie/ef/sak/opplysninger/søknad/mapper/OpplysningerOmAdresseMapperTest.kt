package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.OpplysningerOmAdresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OpplysningerOmAdresseMapperTest {

    @Test
    internal fun `mapper til null hvis objektet er null`() {
        val dto = OpplysningerOmAdresseMapper.tilDto(null)
        assertThat(dto).isNull()
    }

    @Test
    internal fun `mapper til null hvis man ikke besvart søkerBorPåRegistrertAdresse då det alltid besvares før harMeldtFlytteendring`() {
        val dto = OpplysningerOmAdresseMapper.tilDto(OpplysningerOmAdresse("", null, null))
        assertThat(dto).isNull()
    }

    @Test
    internal fun `skal mappe verdier`() {
        val opplysningerOmAdresse = OpplysningerOmAdresse(
            adresse = "adresse",
            søkerBorPåRegistrertAdresse = false,
            harMeldtFlytteendring = true,
            dokumentasjonFlytteendring = null
        )
        val dto = OpplysningerOmAdresseMapper.tilDto(opplysningerOmAdresse)!!
        assertThat(dto.adresse).isEqualTo("adresse")
        assertThat(dto.søkerBorPåRegistrertAdresse).isFalse
        assertThat(dto.harMeldtFlytteendring).isTrue
    }
}