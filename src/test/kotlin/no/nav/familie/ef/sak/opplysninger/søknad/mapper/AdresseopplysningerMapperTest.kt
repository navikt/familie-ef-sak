package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Adresseopplysninger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AdresseopplysningerMapperTest {

    @Test
    internal fun `mapper til null hvis objektet er null`() {
        val dto = AdresseopplysningerMapper.tilDto(null)
        assertThat(dto).isNull()
    }

    @Test
    internal fun `mapper til null hvis man ikke besvart søkerBorPåRegistrertAdresse då det alltid besvares før harMeldtAdresseendring`() {
        val dto = AdresseopplysningerMapper.tilDto(Adresseopplysninger("", null, null))
        assertThat(dto).isNull()
    }

    @Test
    internal fun `skal mappe verdier`() {
        val adresseopplysninger = Adresseopplysninger(
            adresse = "adresse",
            søkerBorPåRegistrertAdresse = false,
            harMeldtAdresseendring = true,
            dokumentasjonAdresseendring = null
        )
        val dto = AdresseopplysningerMapper.tilDto(adresseopplysninger)!!
        assertThat(dto.adresse).isEqualTo("adresse")
        assertThat(dto.søkerBorPåRegistrertAdresse).isFalse
        assertThat(dto.harMeldtAdresseendring).isTrue
    }
}