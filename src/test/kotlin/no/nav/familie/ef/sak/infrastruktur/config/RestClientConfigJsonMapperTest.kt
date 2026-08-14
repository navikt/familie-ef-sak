package no.nav.familie.ef.sak.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.mockk
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.YearMonth

/**
 * Regresjonstest for feilen der felter som starter med æ/ø/å (f.eks. årMånedFra) forsvant fra
 * JSON-en når en RestClient bygges med RestClient.builder() direkte (slik alle RestClient-bønnene
 * i RestClientConfig gjør, enten direkte eller via EntraIDRestClientFactory). Uten
 * medJsonMapperFraFellesKontrakter() bruker Spring sin default JsonMapper, som mangler
 * KotlinFeature.KotlinPropertyNameAsImplicitName, og da mister Kotlin-modulen navnet på felter som
 * starter med æ/ø/å helt (feltet blir ikke bare feilaktig navngitt, men utelatt fra JSON-en).
 */
class RestClientConfigJsonMapperTest {
    data class DtoMedÆøåFelt(
        val årMånedFra: YearMonth,
        val vanligFelt: String,
    )

    @AfterEach
    fun tearDownEachTest() {
        wiremockServer.resetAll()
    }

    @Test
    fun `utenAuthRestClient serialiserer felter som starter med æøå riktig`() {
        wiremockServer.stubFor(
            post(urlEqualTo("/test"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{}"),
                ),
        )

        val restClient = restClientConfig.utenAuthRestClient()
        restClient
            .post()
            .uri(URI.create("${wiremockServer.baseUrl()}/test"))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(DtoMedÆøåFelt(YearMonth.of(2024, 1), "verdi"))
            .retrieve()
            .toBodilessEntity()

        val body =
            wiremockServer
                .findAll(postRequestedFor(urlEqualTo("/test")))
                .single()
                .bodyAsString
        assertThat(body).contains("\"årMånedFra\":\"2024-01\"")
    }

    @Test
    fun `default RestClient uten fix mister felt som starter med æøå (dokumenterer bugen)`() {
        wiremockServer.stubFor(
            post(urlEqualTo("/test-uten-fix"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{}"),
                ),
        )

        val restClientUtenFix = RestClient.builder().build()
        restClientUtenFix
            .post()
            .uri(URI.create("${wiremockServer.baseUrl()}/test-uten-fix"))
            .body(DtoMedÆøåFelt(YearMonth.of(2024, 1), "verdi"))
            .retrieve()
            .toBodilessEntity()

        val body =
            wiremockServer
                .findAll(postRequestedFor(urlEqualTo("/test-uten-fix")))
                .single()
                .bodyAsString
        assertThat(body).doesNotContain("årMånedFra")
        assertThat(body).contains("vanligFelt")
    }

    companion object {
        private lateinit var wiremockServer: WireMockServer
        private lateinit var restClientConfig: RestClientConfig

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServer.start()

            restClientConfig =
                RestClientConfig(
                    entraIDRestClientFactory = mockk<EntraIDRestClientFactory>(),
                    consumerIdClientInterceptor = ConsumerIdClientInterceptor("familie-ef-sak", "test"),
                    mdcValuesPropagatingClientInterceptor = MdcValuesPropagatingClientInterceptor(),
                    jsonMapper = JsonMapperProvider.jsonMapper,
                )
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServer.stop()
        }
    }
}
