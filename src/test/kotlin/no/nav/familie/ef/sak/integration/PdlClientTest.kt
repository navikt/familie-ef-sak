package no.nav.familie.ef.sak.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.exception.PdlRequestException
import no.nav.familie.http.sts.StsRestClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

class PdlClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var pdlClient: PdlClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            val stsRestClient = mockk<StsRestClient>()
            every { stsRestClient.systemOIDCToken } returns "token"
            pdlClient = PdlClient(PdlConfig(URI.create(wiremockServerItem.baseUrl())), restOperations, stsRestClient)

        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }

    @Test
    fun `pdlClient håndterer response for søker-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("søker.json"))))

        val response = pdlClient.hentSøker("")

        assertThat(response.bostedsadresse[0].vegadresse?.adressenavn).isEqualTo("INNGJERDSVEGEN")
    }

    @Test
    fun `pdlClient håndterer response for andreForeldre-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("andreForeldre.json"))))

        val response = pdlClient.hentAndreForeldre(listOf("11111122222"))

        assertThat(response["11111122222"]?.bostedsadresse?.get(0)?.angittFlyttedato).isEqualTo(LocalDate.of(1966, 11, 18))
    }

    @Test
    fun `pdlClient håndterer response for barn-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("barn.json"))))

        val response = pdlClient.hentBarn(listOf("11111122222"))

        assertThat(response["11111122222"]?.fødsel?.get(0)?.fødselsdato).isEqualTo(LocalDate.of(1966, 11, 18))
    }

    @Test
    fun `pdlClient håndterer response for søkerKort-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("søker_kort_bolk.json"))))

        val response = pdlClient.hentSøkerKortBolk(listOf("11111122222"))

        assertThat(response["11111122222"]?.navn?.get(0)?.fornavn).isEqualTo("BRÅKETE")
    }

    @Test
    fun `pdlClient håndterer response for personKortBolk-query mot pdl-tjenesten riktig`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("person_kort_bolk.json"))))

        val response = pdlClient.hentPersonKortBolk(listOf("11111122222"))

        assertThat(response["11111122222"]?.navn?.get(0)?.fornavn).isEqualTo("BRÅKETE")
    }

    @Test
    fun `pdlClient håndterer response for uthenting av identer`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("hent_identer.json"))))
        val response = pdlClient.hentAktørId("12345")
        assertThat(response.identer.first().ident).isEqualTo("12345678901")
    }

    @Test
    fun `pdlClient håndterer response for søker-query mot pdl-tjenesten der person i data er null`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson("{\"data\": {}}")))
        assertThat(Assertions.catchThrowable { pdlClient.hentSøker("") })
                .hasMessageStartingWith("Manglende ")
                .isInstanceOf(PdlRequestException::class.java)
    }

    @Test
    fun `pdlClient håndterer response for søker-query mot pdl-tjenesten der data er null og har errors`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("pdlErrorResponse.json"))))
        assertThat(Assertions.catchThrowable { pdlClient.hentSøker("") })
                .hasMessageStartingWith("Feil ved henting av")
                .isInstanceOf(PdlRequestException::class.java)
    }

    @Test
    fun `pdlClient håndterer response for bolk-query mot pdl-tjenesten der person er null og har errors`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("pdlBolkErrorResponse.json"))))
        assertThat(Assertions.catchThrowable { pdlClient.hentBarn(listOf("")) })
                .hasMessageStartingWith("Feil ved henting av")
                .isInstanceOf(PdlRequestException::class.java)
    }

    @Test
    fun `pdlClient håndterer response for bolk-query mot pdl-tjenesten der data er null og har errors`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("pdlBolkErrorResponse_nullData.json"))))
        assertThat(Assertions.catchThrowable { pdlClient.hentBarn(listOf("")) })
                .hasMessageStartingWith("Data er null fra PDL")
                .isInstanceOf(PdlRequestException::class.java)
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/json/$filnavn").readText()
    }
}
