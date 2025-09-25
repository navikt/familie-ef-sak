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
        Assertions.assertThat(response).isNotNull
        Assertions.assertThat(response.size).isEqualTo(1)
    }

    private val queryMappingForHentOrganisasjon: MappingBuilder =
        WireMock.get(WireMock.urlPathEqualTo("/api/v2/arbeidstaker/arbeidsforhold"))

    val aaregIntegrasjonerResponse =
        """
         [
          {
            "id": "V911050676R16054L0001",
            "type": {
              "kode": "ordinaertArbeidsforhold",
              "beskrivelse": "Ordinært arbeidsforhold"
            },
            "arbeidstaker": {
              "identer": [
                {
                  "type": "AKTORID",
                  "ident": "2175141353812",
                  "gjeldende": true
                },
                {
                  "type": "FOLKEREGISTERIDENT",
                  "ident": "30063000562",
                  "gjeldende": true
                }
              ]
            },
            "arbeidssted": {
              "type": "Underenhet",
              "identer": [
                {
                  "type": "ORGANISASJONSNUMMER",
                  "ident": "910825518"
                }
              ]
            },
            "opplysningspliktig": {
              "type": "Hovedenhet",
              "identer": [
                {
                  "type": "ORGANISASJONSNUMMER",
                  "ident": "810825472"
                }
              ]
            },
            "ansettelsesperiode": {
              "startdato": "2014-01-01"
            },
            "ansettelsesdetaljer": [
              {
                "type": "Ordinaer",
                "arbeidstidsordning": {
                  "kode": "ikkeSkift",
                  "beskrivelse": "Ikke skift"
                },
                "ansettelsesform": {
                  "kode": "fast",
                  "beskrivelse": "Fast ansettelse"
                },
                "yrke": {
                  "kode": "1231119",
                  "beskrivelse": "KONTORLEDER"
                },
                "antallTimerPrUke": 37.5,
                "avtaltStillingsprosent": 100,
                "sisteStillingsprosentendring": "2014-01-01",
                "sisteLoennsendring": "2014-01-01",
                "rapporteringsmaaneder": {
                  "fra": "2019-11",
                  "til": null
                }
              },
              {
                "type": "Ordinaer",
                "arbeidstidsordning": {
                  "kode": "ikkeSkift",
                  "beskrivelse": "Ikke skift"
                },
                "ansettelsesform": {
                  "kode": "fast",
                  "beskrivelse": "Fast ansettelse"
                },
                "yrke": {
                  "kode": "1231119",
                  "beskrivelse": "KONTORLEDER"
                },
                "antallTimerPrUke": 37.5,
                "avtaltStillingsprosent": 100,
                "sisteStillingsprosentendring": "2016-01-01",
                "sisteLoennsendring": "2016-01-01",
                "rapporteringsmaaneder": {
                  "fra": "2016-01",
                  "til": "2019-10"
                }
              }
            ],
            "permisjoner": [
              {
                "id": "68796",
                "type": {
                  "kode": "permisjonMedForeldrepenger",
                  "beskrivelse": "Permisjon med foreldrepenger"
                },
                "startdato": "2021-01-29",
                "prosent": 50
              }
            ],
            "permitteringer": [
              {
                "id": "54232",
                "type": {
                  "kode": "permittering",
                  "beskrivelse": "Permittering"
                },
                "startdato": "2020-10-30",
                "prosent": 50
              }
            ],
            "rapporteringsordning": {
              "kode": "a-ordningen",
              "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
            },
            "navArbeidsforholdId": 12345,
            "navVersjon": 5,
            "navUuid": "28199f29-29e3-42fc-8784-049772bc72fe",
            "opprettet": "2020-05-28T08:52:01.793",
            "sistBekreftet": "2020-09-15T08:19:53",
            "sistEndret": "2020-07-03T14:13:00",
            "bruksperiode": {
              "fom": "2020-07-03T14:06:00.286",
              "tom": null
            }
          }
        ]
        """.trimIndent()
}
