package no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg

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
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestOperations
import java.net.URI

class EregClientTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var eregClient: EregClient

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            eregClient = EregClient(URI.create(server.baseUrl()), restOperations)
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
    fun `hent organisasjon response`() {
        WireMock.stubFor(
            queryMappingForHentOrganisasjon.willReturn(
                WireMock
                    .aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                    .withBody(eregHentOrganisasjonResponse),
            ),
        )

        val response = eregClient.hentOrganisasjoner(orgnumre)
        assertThat(response).isNotNull
        assertThat(response.size).isEqualTo(2)
        assertThat(response[0].organisasjonsnummer).isEqualTo(orgnumre[0])
        assertThat(response[0].navn.redigertnavn).isEqualTo("PENGELØS SPAREBANK")

        assertThat(response[1].organisasjonsnummer).isEqualTo(orgnumre[1])
        assertThat(response[1].navn.redigertnavn).isEqualTo("SJOKKERENDE ELEKTRIKER")
    }

    private val orgnumre = listOf("972674818", "999999929")

    private val queryMappingForHentOrganisasjon: MappingBuilder = WireMock.get(WireMock.urlPathEqualTo("/api/ereg"))

    private val eregHentOrganisasjonResponse =
        """
        [
            {
                "organisasjonsnummer": "972674818",
                "type": "Virksomhet",
                "navn": {
                    "redigertnavn": "PENGELØS SPAREBANK",
                    "navnelinje1": "PENGELØS SPAREBANK",
                    "bruksperiode": {
                        "fom": "2021-06-02T09:23:59.803"
                    },
                    "gyldighetsperiode": {
                        "fom": "2021-06-02"
                    }
                },
                "organisasjonDetaljer": {
                    "registreringsdato": "2020-03-31T00:00:00",
                    "enhetstyper": [
                        {
                            "enhetstype": "BEDR",
                            "bruksperiode": {
                                "fom": "2020-03-31T13:37:24.419"
                            },
                            "gyldighetsperiode": {
                                "fom": "2020-03-31"
                            }
                        }
                    ],
                    "navn": [
                        {
                            "redigertnavn": "PENGELØS SPAREBANK",
                            "navnelinje1": "PENGELØS SPAREBANK",
                            "bruksperiode": {
                                "fom": "2021-06-02T09:23:59.803"
                            },
                            "gyldighetsperiode": {
                                "fom": "2021-06-02"
                            }
                        }
                    ],
                    "forretningsadresser": [
                        {
                            "type": "Forretningsadresse",
                            "adresselinje1": "BOLSTADVEIEN 30",
                            "postnummer": "4532",
                            "poststed": "ØYSLEBØ",
                            "landkode": "NO",
                            "kommunenummer": "4205",
                            "bruksperiode": {
                                "fom": "2021-06-02T09:23:59.805"
                            },
                            "gyldighetsperiode": {
                                "fom": "2021-06-02"
                            }
                        }
                    ],
                    "postadresser": [
                        {
                            "type": "Postadresse",
                            "adresselinje1": "BOLSTADVEIEN 30",
                            "postnummer": "4532",
                            "poststed": "ØYSLEBØ",
                            "landkode": "NO",
                            "kommunenummer": "4205",
                            "bruksperiode": {
                                "fom": "2021-06-02T09:23:59.807"
                            },
                            "gyldighetsperiode": {
                                "fom": "2021-06-02"
                            }
                        }
                    ],
                    "sistEndret": "2021-06-02"
                }
            },
            {
                "organisasjonsnummer": "999999929",
                "type": "Virksomhet",
                "navn": {
                    "redigertnavn": "SJOKKERENDE ELEKTRIKER",
                    "navnelinje1": "SJOKKERENDE ELEKTRIKER",
                    "bruksperiode": {
                        "fom": "2021-06-02T09:23:59.839"
                    },
                    "gyldighetsperiode": {
                        "fom": "2021-06-02"
                    }
                },
                "organisasjonDetaljer": {
                    "registreringsdato": "2021-06-02T00:00:00",
                    "enhetstyper": [
                        {
                            "enhetstype": "BEDR",
                            "bruksperiode": {
                                "fom": "2021-06-02T09:21:17.472"
                            },
                            "gyldighetsperiode": {
                                "fom": "2021-06-02"
                            }
                        }
                    ],
                    "navn": [
                        {
                            "redigertnavn": "SJOKKERENDE ELEKTRIKER",
                            "navnelinje1": "SJOKKERENDE ELEKTRIKER",
                            "bruksperiode": {
                                "fom": "2021-06-02T09:23:59.839"
                            },
                            "gyldighetsperiode": {
                                "fom": "2021-06-02"
                            }
                        }
                    ],
                    "forretningsadresser": [
                        {
                            "type": "Forretningsadresse",
                            "adresselinje1": "DRIVHUSVEGEN 40",
                            "postnummer": "9027",
                            "poststed": "RAMFJORDBOTN",
                            "landkode": "NO",
                            "kommunenummer": "5401",
                            "bruksperiode": {
                                "fom": "2021-06-02T09:23:59.84"
                            },
                            "gyldighetsperiode": {
                                "fom": "2021-06-02"
                            }
                        }
                    ],
                    "postadresser": [
                        {
                            "type": "Postadresse",
                            "adresselinje1": "DRIVHUSVEGEN 40",
                            "postnummer": "9027",
                            "poststed": "RAMFJORDBOTN",
                            "landkode": "NO",
                            "kommunenummer": "5401",
                            "bruksperiode": {
                                "fom": "2021-06-02T09:23:59.841"
                            },
                            "gyldighetsperiode": {
                                "fom": "2021-06-02"
                            }
                        }
                    ],
                    "sistEndret": "2021-06-02"
                }
            }
        ]
        """.trimIndent()
}
