package no.nav.familie.ef.sak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

internal class IntegrasjonerConfigTest {

    private val integrasjonerConfig = IntegrasjonerConfig(URI("http://familie-integrasjoner"))

    @Test
    fun getPingUri() {
        assertThat(integrasjonerConfig.pingUri).isEqualTo(URI("http://familie-integrasjoner/internal/status/isAlive"))
    }

    @Test
    fun getTilgangUri() {
        assertThat(integrasjonerConfig.tilgangUri).isEqualTo(URI("http://familie-integrasjoner/api/tilgang/personer"))
    }

    @Test
    fun getPersonopplysninger() {
        assertThat(integrasjonerConfig.personopplysningerUri)
                .isEqualTo(URI("http://familie-integrasjoner/api/personopplysning/v1/info"))
    }

    @Test
    fun getPersonhistorikk() {
        assertThat(integrasjonerConfig.personhistorikkUri)
                .isEqualTo(URI("http://familie-integrasjoner/api/personopplysning/v1/historikk"))
    }
}