package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.OpplysningerOmAdresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OpplysningerOmAdresseMapperTest {

    @Test
    internal fun `mapper til tomt objekt hvis null`() {
        val dto = OpplysningerOmAdresseMapper.tilDto(null)
        assertThat(dto.adresse).isNull()
        assertThat(dto.søkerBorPåRegistrertAdresse).isNull()
        assertThat(dto.harMeldtFlytteendring).isNull()
    }

    @Test
    internal fun `skal mappe verdier`() {
        val opplysningerOmAdresse = OpplysningerOmAdresse(
            adresse = "adresse",
            søkerBorPåRegistrertAdresse = false,
            harMeldtFlytteendring = true,
            dokumentasjonFlytteendring = null
        )
        val dto = OpplysningerOmAdresseMapper.tilDto(opplysningerOmAdresse)
        assertThat(dto.adresse).isEqualTo("adresse")
        assertThat(dto.søkerBorPåRegistrertAdresse).isFalse
        assertThat(dto.harMeldtFlytteendring).isTrue
    }
}