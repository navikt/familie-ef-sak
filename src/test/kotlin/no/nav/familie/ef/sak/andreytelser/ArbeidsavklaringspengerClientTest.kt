package no.nav.familie.ef.sak.no.nav.familie.ef.sak.andreytelser

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.andreytelser.ArbeidsavklaringspengerClient
import no.nav.familie.ef.sak.andreytelser.ArbeidsavklaringspengerRequest
import org.apache.hc.core5.http.ContentType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

class ArbeidsavklaringspengerClientTest {
    companion object {
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var arbeidsavklaringspengerClient: ArbeidsavklaringspengerClient
        lateinit var wiremockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun start() {
            wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServer.start()
            arbeidsavklaringspengerClient =
                ArbeidsavklaringspengerClient(
                    URI.create(wiremockServer.baseUrl()),
                    restOperations,
                )
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            wiremockServer.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(wiremockServer.port())
    }

    @Test
    fun `skal hente perioder for arbeidsavklaringspenger`() {
        wiremockServer.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/maksimumUtenUtbetaling"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(arbeidsavklaringspengerResponse),
                ),
        )

        val request =
            ArbeidsavklaringspengerRequest(
                LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(1),
                "",
            )

        val response = arbeidsavklaringspengerClient.hentPerioder(request)

        Assertions.assertThat(response).isNotNull
        Assertions.assertThat(response.vedtak.size).isEqualTo(1)
        Assertions.assertThat(response.vedtak.first().dagsats).isEqualTo(1982)
    }

    val arbeidsavklaringspengerResponse =
        """
            {
  "vedtak": [
    {
      "dagsats": 1982,
      "dagsatsEtterUføreReduksjon": 0,
      "vedtakId": "73193",
      "status": "LØPENDE",
      "saksnummer": "4oKCNC0",
      "vedtaksdato": "2025-09-05",
      "periode": {
        "fraOgMedDato": "2025-09-05",
        "tilOgMedDato": "2026-09-04"
      },
      "rettighetsType": "BISTANDSBEHOV",
      "beregningsgrunnlag": 780960,
      "barnMedStonad": 3,
      "barnetillegg": 111,
      "kildesystem": "KELVIN",
      "samordningsId": null,
      "opphorsAarsak": null,
      "vedtaksTypeKode": null,
      "vedtaksTypeNavn": null,
      "utbetaling": []
    }
  ]
}
        """.trimIndent()
}
