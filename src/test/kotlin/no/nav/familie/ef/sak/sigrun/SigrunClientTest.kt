package no.nav.familie.ef.sak.no.nav.familie.ef.sak.sigrun

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import org.apache.http.entity.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

class SigrunClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        private val webClient: WebClient = WebClient.create()
        lateinit var sigrunClient: SigrunClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            sigrunClient = SigrunClient(URI.create(wiremockServerItem.baseUrl()), restOperations, webClient, mockFeatureToggleService())
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
    fun `hent beregnetskatt fra sigrun og map til objekt`() {
        wiremockServerItem.stubFor(
            WireMock.post(urlEqualTo("/api/v1/beregnetskatt?inntektsaar=2022"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(beregnetSkattRessursResponseJson)
                )
        )
        val beregnetSkatt = sigrunClient.hentBeregnetSkatt("123", 2022)
        assertThat(beregnetSkatt.size).isEqualTo(7)
        assertThat(beregnetSkatt.first().verdi).isEqualTo("814952")
        assertThat(beregnetSkatt.first().tekniskNavn).isEqualTo("personinntektFiskeFangstFamiliebarnehage")
    }

    @Test
    fun `hent summertskattegrunnlag fra sigrun og map til objekt`() {
        wiremockServerItem.stubFor(
            WireMock.post(urlEqualTo("/api/v1/summertskattegrunnlag?inntektsaar=2018"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(summertSkattegrunnlagJson)
                )
        )
        val summertSkattegrunnlag = sigrunClient.hentSummertSkattegrunnlag("123", 2018)
        assertThat(summertSkattegrunnlag.grunnlag.size).isEqualTo(4)
        assertThat(summertSkattegrunnlag.skatteoppgjoersdato).isEqualTo("2018-10-04")
        assertThat(summertSkattegrunnlag.svalbardGrunnlag.size).isEqualTo(4)
        assertThat(summertSkattegrunnlag.svalbardGrunnlag.first().tekniskNavn).isEqualTo("samledePaaloepteRenter")
        assertThat(summertSkattegrunnlag.svalbardGrunnlag.first().beloep).isEqualTo(779981)
    }

    private val beregnetSkattRessursResponseJson = """
        [
          {
            "tekniskNavn": "personinntektFiskeFangstFamiliebarnehage",
            "verdi": "814952"
          },
          {
            "tekniskNavn": "personinntektNaering",
            "verdi": "785896"
          },
          {
            "tekniskNavn": "personinntektBarePensjonsdel",
            "verdi": "844157"
          },
          {
            "tekniskNavn": "svalbardLoennLoennstrekkordningen",
            "verdi": "874869"
          },
          {
            "tekniskNavn": "personinntektLoenn",
            "verdi": "746315"
          },
          {
            "tekniskNavn": "svalbardPersoninntektNaering",
            "verdi": "696009"
          },
          {
            "tekniskNavn": "skatteoppgjoersdato",
            "verdi": "2017-08-09"
          }
        ]
    """

    private val summertSkattegrunnlagJson = """
        {
          "grunnlag": [
            {
              "tekniskNavn": "samledePaaloepteRenter",
              "beloep": 779981
            },
            {
              "tekniskNavn": "andreFradragsberettigedeKostnader",
              "beloep": 59981
            },
            {
              "tekniskNavn": "samletSkattepliktigOverskuddAvUtleieAvFritidseiendom",
              "beloep": 1609981
            },
            {
              "tekniskNavn": "skattepliktigAvkastningEllerKundeutbytte",
              "beloep": 1749981
            }
          ],
          "skatteoppgjoersdato": "2018-10-04",
          "svalbardGrunnlag": [
            {
              "tekniskNavn": "samledePaaloepteRenter",
              "beloep": 779981
            },
            {
              "tekniskNavn": "samletAndelAvInntektIBoligselskapEllerBoligsameie",
              "beloep": 849981
            },
            {
              "tekniskNavn": "loennsinntektMedTrygdeavgiftspliktOmfattetAvLoennstrekkordningen",
              "beloep": 1779981
            },
            {
              "tekniskNavn": "skattepliktigAvkastningEllerKundeutbytte",
              "beloep": 1749981
            }
          ]
        }
        """
}