package no.nav.familie.ef.sak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

internal class IntegrasjonerConfigTest {

    private val integrasjonerConfig = IntegrasjonerConfig(URI("http://familie-integrasjoner"))

    @Test
    fun getPingUri() {
        assertThat(integrasjonerConfig.pingUri).isEqualTo(URI("http://familie-integrasjoner/api/ping"))
    }

    @Test
    fun getTilgangUri() {
        assertThat(integrasjonerConfig.tilgangRelasjonerUri).isEqualTo(URI("http://familie-integrasjoner/api/tilgang/person-med-relasjoner"))
    }

    @Test
    fun getPersonopplysninger() {
        assertThat(integrasjonerConfig.personopplysningerUri)
                .isEqualTo(URI("http://familie-integrasjoner/api/personopplysning/v2/info"))
    }

    @Test
    fun getPersonhistorikk() {
        assertThat(integrasjonerConfig.personhistorikkUri)
                .isEqualTo(URI("http://familie-integrasjoner/api/personopplysning/v2/historikk"))
    }
}