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
        Assertions.assertThat(response.size).isEqualTo(3)
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
          },
          {
            "id": "b2561ea4-4b40-4633-8370-51152b3bc22e",
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
                  "ident": "972674818"
                }
              ]
            },
            "opplysningspliktig": {
              "type": "Hovedenhet",
              "identer": [
                {
                  "type": "ORGANISASJONSNUMMER",
                  "ident": "928497704"
                }
              ]
            },
            "ansettelsesperiode": {
              "startdato": "2020-01-01"
            },
            "ansettelsesdetaljer": [
              {
                "type": "Ordinaer",
                "arbeidstidsordning": {
                  "kode": "ikkeSkift",
                  "beskrivelse": "Ikke skift"
                },
                "ansettelsesform": {
                  "kode": "midlertidig",
                  "beskrivelse": "Midlertidig ansettelse"
                },
                "yrke": {
                  "kode": "1233101",
                  "beskrivelse": "ABONNEMENTSJEF"
                },
                "antallTimerPrUke": 40,
                "avtaltStillingsprosent": 100,
                "sisteStillingsprosentendring": "2020-01-01",
                "sisteLoennsendring": "2020-01-01",
                "rapporteringsmaaneder": {
                  "fra": "2020-01",
                  "til": null
                }
              }
            ],
            "idHistorikk": [
              {
                "id": "2b84b5cb-2eeb-4c50-8f40-51145b7cf852",
                "bruksperiode": {
                  "fom": "2020-09-09T17:31:45.481",
                  "tom": null
                }
              }
            ],
            "varsler": [
              {
                "entitet": "Arbeidsforhold",
                "varslingskode": {
                  "kode": "AFIDHI",
                  "beskrivelse": "Arbeidsforholdet har id-historikk"
                }
              }
            ],
            "rapporteringsordning": {
              "kode": "a-ordningen",
              "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
            },
            "navArbeidsforholdId": 45678,
            "navVersjon": 6,
            "navUuid": "6c9cb6aa-528a-4f83-ab10-c511fe1fb2fd",
            "opprettet": "2020-06-16T20:50:07.81",
            "sistBekreftet": "2020-07-28T09:10:19",
            "sistEndret": "2020-09-09T17:32:15",
            "bruksperiode": {
              "fom": "2020-07-03T14:11:07.021",
              "tom": null
            }
          },
          {
            "id": "C7COVNUR41H4UYEFIBDQDXB2T",
            "type": {
              "kode": "maritimtArbeidsforhold",
              "beskrivelse": "Maritimt arbeidsforhold"
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
                  "ident": "896929119"
                }
              ]
            },
            "opplysningspliktig": {
              "type": "Hovedenhet",
              "identer": [
                {
                  "type": "ORGANISASJONSNUMMER",
                  "ident": "928497704"
                }
              ]
            },
            "ansettelsesperiode": {
              "startdato": "2012-03-15"
            },
            "ansettelsesdetaljer": [
              {
                "type": "Maritim",
                "fartsomraade": {
                  "kode": "innenriks",
                  "beskrivelse": "Innenriks"
                },
                "skipsregister": {
                  "kode": "nor",
                  "beskrivelse": "NOR"
                },
                "fartoeystype": {
                  "kode": "annet",
                  "beskrivelse": "Annet"
                },
                "arbeidstidsordning": {
                  "kode": "helkontinuerligSkiftOgAndreOrdninger336",
                  "beskrivelse": "Helkontinuerlig skiftarbeid og andre ordninger med 33,6 t/u"
                },
                "ansettelsesform": {
                  "kode": "fast",
                  "beskrivelse": "Fast ansettelse"
                },
                "yrke": {
                  "kode": "6411104",
                  "beskrivelse": "FISKER"
                },
                "antallTimerPrUke": 31,
                "avtaltStillingsprosent": 100,
                "sisteStillingsprosentendring": "2012-03-15",
                "sisteLoennsendring": "2020-02-29",
                "rapporteringsmaaneder": {
                  "fra": "2020-02",
                  "til": null
                }
              }
            ],
            "utenlandsopphold": [
              {
                "landkode": {
                  "kode": "AR",
                  "beskrivelse": "ARGENTINA"
                },
                "startdato": "2016-11-01",
                "sluttdato": "2016-11-30",
                "rapporteringsmaaned": "2016-11"
              },
              {
                "landkode": {
                  "kode": "AO",
                  "beskrivelse": "ANGOLA"
                },
                "startdato": "2016-03-01",
                "sluttdato": "2016-03-31",
                "rapporteringsmaaned": "2016-03"
              }
            ],
            "rapporteringsordning": {
              "kode": "a-ordningen",
              "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
            },
            "navArbeidsforholdId": 23456,
            "navVersjon": 1,
            "navUuid": "0c405fc1-36a9-4c52-859f-b695092af7b7",
            "opprettet": "2021-01-01T19:25:17.39",
            "sistBekreftet": "2021-01-01T19:25:17",
            "sistEndret": "2021-01-02T14:27:33",
            "bruksperiode": {
              "fom": "2021-01-02T14:27:13.086",
              "tom": null
            }
          }
        ]
        """.trimIndent()
}
