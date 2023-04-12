package no.nav.familie.ef.sak.kontantstøtte

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.apache.hc.core5.http.ContentType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

class KontantstøtteClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var kontantstøtteClient: KontantstøtteClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            kontantstøtteClient = KontantstøtteClient(URI.create(wiremockServerItem.baseUrl()), restOperations)
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
    fun `hent utbetalingsinfo kontantstøtte og map response`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/api/bisys/hent-utbetalingsinfo"))
                .withRequestBody(WireMock.equalToJson(hentUtbetalingsinfoRequestJson))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(hentUtbetalingsinfoResponseJson),
                ),
        )
        val utbetalingsinfo = kontantstøtteClient.hentUtbetalingsinfo(listOf("01010199999", "02020299999"))
        Assertions.assertThat(utbetalingsinfo.size).isEqualTo(2)
    }

    private val fomDatoRequest = LocalDate.MIN.toString()
    private val hentUtbetalingsinfoRequestJson = """
        {
          "fom": "$fomDatoRequest",
          "identer": [
            "01010199999",
            "02020299999"
          ]
        }
    """.trimIndent()

    private val hentUtbetalingsinfoResponseJson = """
        [
          {
            "infotrygdPerioder": [
              {
                "fomMåned": "2022-09",
                "tomMåned": "2022-10",
                "beløp": 1000,
                "barna": [
                  "01010199999"
                ]
              }
            ],
            "ksSakPerioder": [
              {
                "fomMåned": "2022-11",
                "tomMåned": "2022-12",
                "barn": {
                  "beløp": 2000,
                  "ident": "01010199999"
                }
              }
            ]
          },
          {
            "infotrygdPerioder": [
              {
                "fomMåned": "2023-01",
                "tomMåned": "2023-02",
                "beløp": 3000,
                "barna": [
                  "02020299999"
                ]
              }
            ],
            "ksSakPerioder": [
              {
                "fomMåned": "2023-03",
                "tomMåned": "2023-04",
                "barn": {
                  "beløp": 4000,
                  "ident": "02020299999"
                }
              }
            ]
          }
        ]
    """.trimIndent()
}
