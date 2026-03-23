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
import org.springframework.boot.restclient.RestTemplateBuilder
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
                .post(urlEqualTo("/api/sigrun/pensjonsgivendeinntekt?inntektsaar=2022"))
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

    @Test
    fun `hent beregnetskatt fra sigrun og map til objekt`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/api/sigrun/beregnetskatt?inntektsaar=2022"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(beregnetSkattRessursResponseJson),
                ),
        )
        val beregnetSkatt = sigrunClient.hentBeregnetSkatt("123", 2022)
        assertThat(beregnetSkatt.size).isEqualTo(7)
        assertThat(beregnetSkatt.first().verdi).isEqualTo("814952")
        assertThat(beregnetSkatt.first().tekniskNavn).isEqualTo("personinntektFiskeFangstFamiliebarnehage")
    }

    @Test
    fun `hent beregnetskatt fra sigrun og map til objekt med skatteoppgjørsdato`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/api/sigrun/beregnetskatt?inntektsaar=2022"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(beregnetSkattMedOppgjørsdatoJson),
                ),
        )
        val beregnetSkatt = sigrunClient.hentBeregnetSkatt("123", 2022)
        assertThat(beregnetSkatt.size).isEqualTo(2)
        assertThat(beregnetSkatt.last().verdi).isEqualTo("200000")
        assertThat(beregnetSkatt.last().tekniskNavn).isEqualTo("personinntektNaering")
    }

    @Test
    fun `hent summertskattegrunnlag fra sigrun og map til objekt`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/api/sigrun/summertskattegrunnlag?inntektsaar=2018"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.mimeType)
                        .withBody(summertSkattegrunnlagJson),
                ),
        )
        val summertSkattegrunnlag = sigrunClient.hentSummertSkattegrunnlag("123", 2018)
        assertThat(summertSkattegrunnlag.grunnlag.size).isEqualTo(4)
        assertThat(summertSkattegrunnlag.skatteoppgjoersdato).isEqualTo("2018-10-04")
        assertThat(summertSkattegrunnlag.svalbardGrunnlag.size).isEqualTo(4)
        assertThat(summertSkattegrunnlag.svalbardGrunnlag.first().tekniskNavn).isEqualTo("samledePaaloepteRenter")
        assertThat(summertSkattegrunnlag.svalbardGrunnlag.first().beloep).isEqualTo(779981)
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

    private val beregnetSkattMedOppgjørsdatoJson =
        """
        [
          {
            "tekniskNavn": "skatteoppgjoersdato",
            "verdi": "2022-05-01"
          },
          {
            "tekniskNavn": "personinntektNaering",
            "verdi": "200000"
          }
        ]
        """.trimIndent()

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
