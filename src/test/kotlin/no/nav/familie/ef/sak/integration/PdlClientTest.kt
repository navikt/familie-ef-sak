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
    fun `pdlClienten håndterer response fra pdltjenesten riktig`() {
        wireMockServer.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                       .willReturn(okJson("""{
  "data": {
    "person": {
      "adressebeskyttelse": [
        {
          "gradering": "FORTROLIG"
        }
      ],
      "doedsfall": [],
      "folkeregisterpersonstatus": [],
      "kjoenn": [
        {
          "kjoenn": "MANN"
        }
      ],
      "navn": [
        {
          "fornavn": "FRODIG",
          "mellomnavn": "SLØVENDE",
          "etternavn": "GYNGEHEST"
        }
      ]
    }
  }
}""")))
        val response = pdlClient.hentSøkerKort("")
        assertThat(response.adressebeskyttelse[0].gradering).isEqualTo(AdressebeskyttelseGradering.FORTROLIG)
    }
}