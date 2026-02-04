package no.nav.familie.ef.sak.no.nav.familie.ef.sak.sigrun.ekstern

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.Skatteordning
import org.apache.hc.core5.http.ContentType
import org.assertj.core.api.Assertions.assertThat
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

class SigrunClientTest {
    companion object {
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var sigrunClient: SigrunClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            sigrunClient = SigrunClient(URI.create(wiremockServerItem.baseUrl()), restOperations)
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
    fun `hent pensjonsgivende inntekt fra sigrun og map til objekt`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/v1/pensjonsgivendeinntektforfolketrygden"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(pensjonsgivendeInntektResponseJson),
                ),
        )
        val pensjonsgivendeInntektResponse = sigrunClient.hentPensjonsgivendeInntekt("09528731462", 2022)
        assertThat(pensjonsgivendeInntektResponse.inntektsaar).isEqualTo(2022)
        assertThat(pensjonsgivendeInntektResponse.norskPersonidentifikator).isEqualTo("09528731462")
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.size).isEqualTo(2)
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.first()?.skatteordning).isEqualTo(Skatteordning.FASTLAND)
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.first()?.pensjonsgivendeInntektAvLoennsinntekt).isEqualTo(698219)
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.first()?.pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel).isNull()
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.first()?.pensjonsgivendeInntektAvNaeringsinntekt).isEqualTo(150000)
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.first()?.pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage).isEqualTo(85000)
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.first()?.datoForFastsetting).isEqualTo(LocalDate.of(2023, 9, 27))
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.last()?.skatteordning).isEqualTo(Skatteordning.SVALBARD)
        assertThat(pensjonsgivendeInntektResponse.pensjonsgivendeInntekt?.last()?.pensjonsgivendeInntektAvLoennsinntekt).isEqualTo(492160)
    }

    private val pensjonsgivendeInntektResponseJson =
        """
        {
          "norskPersonidentifikator": "09528731462",
          "inntektsaar": 2022,
          "pensjonsgivendeInntekt": [
            {
              "skatteordning": "FASTLAND",
              "datoForFastsetting": "2023-09-27",
              "pensjonsgivendeInntektAvLoennsinntekt": 698219,
              "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel": null,
              "pensjonsgivendeInntektAvNaeringsinntekt": 150000,
              "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage": 85000
            },
            {
              "skatteordning": "SVALBARD",
              "datoForFastsetting": "2023-09-28",
              "pensjonsgivendeInntektAvLoennsinntekt": 492160,
              "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel": null,
              "pensjonsgivendeInntektAvNaeringsinntekt": 2530000,
              "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage": null
            }
          ]
        }
        """.trimIndent()
}
