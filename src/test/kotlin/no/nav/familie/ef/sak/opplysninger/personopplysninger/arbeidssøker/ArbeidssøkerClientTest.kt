package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.apache.hc.core5.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestOperations
import java.net.URI

class ArbeidssøkerClientTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var arbeidssøkerClient: ArbeidssøkerClient

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            arbeidssøkerClient = ArbeidssøkerClient(URI.create(server.baseUrl()), restOperations)
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `hent arbeidssøker-perioder`() {
        WireMock.stubFor(
            queryMappingForHentFullmakt.willReturn(
                WireMock
                    .aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                    .withBody(hentArbeidssøkerResponse),
            ),
        )

        val response = arbeidssøkerClient.hentPerioder("01010199999")
        assertThat(response).isNotNull
    }

    private val queryMappingForHentFullmakt: MappingBuilder = WireMock.post(WireMock.urlPathEqualTo("/api/v1/veileder/arbeidssoekerperioder"))

    private val hentArbeidssøkerResponse =
        """
        [
          {
            "periodeId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            "startet": {
              "tidspunkt": "2021-09-29T11:22:33.444Z",
              "utfoertAv": {
                "type": "UKJENT_VERDI",
                "id": "12345678910"
              },
              "kilde": "string",
              "aarsak": "string",
              "tidspunktFraKilde": {
                "tidspunkt": "2021-09-29T11:20:33.444Z",
                "avviksType": "UKJENT_VERDI"
              }
            },
            "avsluttet": {
              "tidspunkt": "2021-09-29T11:24:33.444Z",
              "utfoertAv": {
                "type": "UKJENT_VERDI",
                "id": "12345678910"
              },
              "kilde": "string",
              "aarsak": "string",
              "tidspunktFraKilde": {
                "tidspunkt": "2021-09-29T11:20:33.444Z",
                "avviksType": "UKJENT_VERDI"
              }
            }
          }
        ]
        """.trimIndent()
}
