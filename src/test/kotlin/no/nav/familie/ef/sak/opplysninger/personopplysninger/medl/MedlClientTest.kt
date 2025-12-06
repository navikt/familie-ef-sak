package no.nav.familie.ef.sak.no.nav.familie.ef.sak.opplysninger.personopplysninger.medl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.MedlClient
import org.apache.hc.core5.http.ContentType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestOperations
import java.net.URI

class MedlClientTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var medlClient: MedlClient

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            medlClient =
                MedlClient(
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
    fun `test medl periode soek kall`() {
        WireMock.stubFor(
            WireMock
                .post(urlEqualTo("/rest/v1/periode/soek"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(medlResponse),
                ),
        )

        val personIdent = "01010199999"
        val response = medlClient.hentMedlemskapsUnntak(personIdent)
        Assertions.assertThat(response).isNotNull
        Assertions.assertThat(response).isNotNull
        Assertions.assertThat(response.size).isEqualTo(1)
        Assertions.assertThat(response.first().medlem).isEqualTo(true)
        Assertions.assertThat(response.first().dekning).isEqualTo("Full")
    }

    private val medlResponse =
        """
        [
          {
            "unntakId": 111164642,
            "ident": "01010199999",
            "fraOgMed": "2018-08-16",
            "tilOgMed": "2019-06-15",
            "status": "GYLD",
            "dekning": "Full",
            "helsedel": true,
            "medlem": true,
            "lovvalgsland": "NOR",
            "lovvalg": "ENDL",
            "grunnlag": "FTL_2-5",
            "sporingsinformasjon": {
              "versjon": 0,
              "registrert": "2018-06-03",
              "besluttet": "2018-07-16",
              "kilde": "LAANEKASSEN",
              "kildedokument": "Henv_Soknad",
              "opprettet": "2017-01-05T12:20:38",
              "opprettetAv": "BMEDL2003",
              "sistEndret": "2018-06-28T12:20:38",
              "sistEndretAv": "REG-150"
            },
            "studieinformasjon": {
              "statsborgerland": "NOR",
              "studieland": "USA",
              "delstudie": true,
              "soeknadInnvilget": true
            }
          }
        ]
        """.trimIndent()
}
