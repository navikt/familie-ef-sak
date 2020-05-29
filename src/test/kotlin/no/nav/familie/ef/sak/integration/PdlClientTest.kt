package no.nav.familie.ef.sak.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.config.PdlConfig
import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering
import no.nav.familie.http.sts.StsRestClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

class PdlClientTest {

    private val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
        val wireMockPort: String = wireMockServer.port().toString()
        val stsRestClient = mockk<StsRestClient>()
        every { stsRestClient.systemOIDCToken } returns "token"
        pdlClient = PdlClient(PdlConfig(URI.create(wireMockServer.baseUrl())), restOperations, stsRestClient)
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `pdlClienten håndterer response for søker-query mot pdltjenesten riktig`() {
        wireMockServer.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                       .willReturn(okJson(readfile("søker.json"))))
        val response = pdlClient.hentSøker("")
        assertThat(response.bostedsadresse[0].vegadresse?.adressenavn).isEqualTo("INNGJERDSVEGEN")
    }

    @Test
    fun `pdlClienten håndterer response for annenForelder-query mot pdltjenesten riktig`() {
        wireMockServer.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                       .willReturn(okJson(readfile("søker.json"))))
        val response = pdlClient.hentForelder2("")
        assertThat(response.bostedsadresse[0].angittFlyttedato).isEqualTo(LocalDate.of(1966,11,18))
    }

    @Test
    fun `pdlClienten håndterer response for barn-query mot pdltjenesten riktig`() {
        wireMockServer.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                       .willReturn(okJson(readfile("barn.json"))))
        val response = pdlClient.hentBarn("")
        assertThat(response.fødsel[0].fødselsdato).isEqualTo(LocalDate.of(1966,11,18))
    }

    @Test
    fun `pdlClienten håndterer response for søkerKort-query mot pdltjenesten riktig`() {
        wireMockServer.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                       .willReturn(okJson(readfile("søker_kort.json"))))
        val response = pdlClient.hentSøkerKort("")
        assertThat(response.adressebeskyttelse[0].gradering).isEqualTo(AdressebeskyttelseGradering.FORTROLIG)
    }




    private fun readfile(filnavn: String): String {
        return this::class.java.getResource("/json/$filnavn").readText()
    }



}