package no.nav.familie.ef.sak.no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt.FullmaktClient
import org.apache.hc.core5.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestOperations
import java.time.LocalDate

class FullmaktClientTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var fullmaktClient: FullmaktClient

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            fullmaktClient = FullmaktClient(server.baseUrl(), restOperations)
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
    fun `hent fullmakt`() {
        WireMock.stubFor(
            queryMappingForHentFullmakt.willReturn(
                WireMock
                    .aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                    .withBody(hentFullmaktResponse),
            ),
        )

        val response = fullmaktClient.hentFullmakt("12345678911111")
        assertThat(response).isNotNull
        assertThat(response.size).isEqualTo(1)
        assertThat(response.first().fullmektigsNavn).isEqualTo("fullmektigsNavn")
        assertThat(response.first().fullmektig).isEqualTo("fullmektigIdent")
        assertThat(response.first().gyldigFraOgMed).isEqualTo(LocalDate.of(2024, 7, 8))
        assertThat(response.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2024, 7, 9))
        assertThat(response.first().omraade.size).isEqualTo(1)
        assertThat(
            response
                .first()
                .omraade
                .first()
                .tema,
        ).isEqualTo("ENF")
    }

    private val queryMappingForHentFullmakt: MappingBuilder = WireMock.post(WireMock.urlPathEqualTo("/api/internbruker/fullmakt/fullmaktsgiver"))

    private val hentFullmaktResponse =
        """
        [
          {
            "fullmaktId": 1,
            "registrert": "2024-07-09T09:28:46.897Z",
            "registrertAv": "registrertAv",
            "endret": "2024-07-09T09:28:46.897Z",
            "endretAv": "endretAv",
            "opphoert": true,
            "fullmaktsgiver": "fullmaktsgiverIdent",
            "fullmektig": "fullmektigIdent",
            "omraade": [
              {
                "tema": "ENF",
                "handling": [
                  "LES"
                ]
              }
            ],
            "gyldigFraOgMed": "2024-07-08",
            "gyldigTilOgMed": "2024-07-09",
            "fullmaktUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            "opplysningsId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
            "endringsId": 2,
            "status": "status",
            "kilde": "kilde",
            "fullmaktsgiverNavn": "fullmaktsgiverNavn",
            "fullmektigsNavn": "fullmektigsNavn"
          }
        ]
        """.trimIndent()
}
