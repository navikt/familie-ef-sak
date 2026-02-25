package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

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

class HistoriskPensjonClientTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var client: HistoriskPensjonClient

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            client = HistoriskPensjonClient(URI.create(server.baseUrl()), restOperations)
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
    fun `skal hente historisk pensjon status for person med historikk`() {
        WireMock.stubFor(
            queryForHistoriskPensjonForIdent.willReturn(
                WireMock
                    .aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                    .withBody(harHistoriskPensjonRespons),
            ),
        )

        val response = client.hentHistoriskPensjonStatusForIdent("123456789", emptySet())
        assertThat(response).isNotNull
        assertThat(response.webAppUrl).isNotNull
        assertThat(response.historiskPensjonStatus).isEqualTo(HistoriskPensjonStatus.HAR_HISTORIKK)
    }

    @Test
    fun `skal hente historisk pensjon status med ukjent verdi dersom kallet feiler`() {
        WireMock.stubFor(
            queryForHistoriskPensjonForIdent.willReturn(
                WireMock
                    .aResponse()
                    .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()),
            ),
        )

        val response = client.hentHistoriskPensjonStatusForIdent("123456789", emptySet())
        assertThat(response).isNotNull
        assertThat(response.webAppUrl).isNull()
        assertThat(response.historiskPensjonStatus).isEqualTo(HistoriskPensjonStatus.UKJENT)
    }

    @Test
    fun `skal hente historisk pensjon status for person uten historikk`() {
        WireMock.stubFor(
            queryForHistoriskPensjonForIdent.willReturn(
                WireMock
                    .aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                    .withBody(harIkkeHistoriskPensjonRespons),
            ),
        )

        val response = client.hentHistoriskPensjonStatusForIdent("123456789", emptySet())
        assertThat(response).isNotNull
        assertThat(response.webAppUrl).isNotNull
        assertThat(response.historiskPensjonStatus).isEqualTo(HistoriskPensjonStatus.HAR_IKKE_HISTORIKK)
    }

    private val queryForHistoriskPensjonForIdent: MappingBuilder = WireMock.post(WireMock.urlPathEqualTo("/api/ensligForsoerger/harPensjonsdata"))

    private val harHistoriskPensjonRespons =
        """  
        {
          "harPensjonsdata": true,
          "webAppUrl": "http://historisk-pensjon.nav.no/abc"
        }
    """
    private val harIkkeHistoriskPensjonRespons =
        """  
        {
          "harPensjonsdata": false,
          "webAppUrl": "http://historisk-pensjon.nav.no/abc"
        }
    """
}
