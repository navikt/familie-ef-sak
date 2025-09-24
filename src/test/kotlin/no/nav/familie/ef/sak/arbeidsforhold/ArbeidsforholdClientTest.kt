package no.nav.familie.ef.sak.arbeidsforhold

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdClient
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

class ArbeidsforholdClientTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var arbeidsforholdClient: ArbeidsforholdClient

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            arbeidsforholdClient =
                ArbeidsforholdClient(
                    URI.create(server.baseUrl()),
                    restOperations,
                )
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
    fun `hent arbeidsforhold response`() {
        WireMock.stubFor(
            queryMappingForHentOrganisasjon.willReturn(
                WireMock
                    .aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                    .withBody(aaregIntegrasjonerResponse),
            ),
        )

        val response = arbeidsforholdClient.hentArbeidsforhold("15046713637")
        Assertions.assertThat(response).isNotNull
        Assertions.assertThat(response.data).isNotNull
        Assertions.assertThat(response.data?.size).isEqualTo(1)
    }

    private val queryMappingForHentOrganisasjon: MappingBuilder =
        WireMock.get(WireMock.urlPathEqualTo("/api/v2/arbeidstaker/arbeidsforhold"))

    val aaregIntegrasjonerResponse =
        """
        {
            "data": [
                {
                    "navArbeidsforholdId": 3065622,
                    "arbeidsforholdId": "1",
                    "arbeidstaker": {
                        "type": "Person",
                        "offentligIdent": "15046713637",
                        "aktoerId": "2267345319785"
                    },
                    "arbeidsgiver": {
                        "type": "Organisasjon",
                        "organisasjonsnummer": "972674818",
                        "offentligIdent": null
                    },
                    "type": "ordinaertArbeidsforhold",
                    "ansettelsesperiode": {
                        "periode": {
                            "fom": "2001-03-23",
                            "tom": null
                        },
                        "bruksperiode": {
                            "fom": "2021-03-23",
                            "tom": null
                        }
                    },
                    "arbeidsavtaler": [
                        {
                            "arbeidstidsordning": "ikkeSkift",
                            "yrke": "2521106",
                            "stillingsprosent": 100.0,
                            "antallTimerPrUke": 37.5,
                            "beregnetAntallTimerPrUke": 37.5,
                            "bruksperiode": {
                                "fom": "2021-03-23",
                                "tom": null
                            },
                            "gyldighetsperiode": {
                                "fom": "2001-03-01",
                                "tom": null
                            }
                        }
                    ]
                }
            ],
            "status": "SUKSESS",
            "melding": "Innhenting av data var vellykket",
            "frontendFeilmelding": null,
            "stacktrace": null
        }
        """.trimIndent()
}
