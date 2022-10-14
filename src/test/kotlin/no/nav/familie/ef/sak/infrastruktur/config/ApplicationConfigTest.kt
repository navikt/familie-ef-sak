package no.nav.familie.ef.sak.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.webflux.builder.FAMILIE_WEB_CLIENT_BUILDER
import no.nav.familie.webflux.builder.NaisProxyCustomizer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.postForEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate

@Disabled
internal class ApplicationConfigTest: OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var restTemplateBuilder: RestTemplateBuilder

    @Autowired
    @Qualifier(FAMILIE_WEB_CLIENT_BUILDER)
    private lateinit var familieWebClientBuilder: WebClient.Builder

    @Autowired
    private lateinit var naisProxyCustomizer: ObjectProvider<NaisProxyCustomizer>

    data class TestDto(val dato: LocalDate = LocalDate.of(2020, 1, 1))

    @Test
    internal fun `skal ikke sette opp naisProxyCostumizer`() {
        assertThat(naisProxyCustomizer.ifAvailable).isNull()
    }

    @Test
    internal fun `default restTemplateBuilder skal sende datoer som iso`() {
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.ok()))
        val restTemplate = restTemplateBuilder.build()
        restTemplate.postForEntity<String>("http://localhost:${wiremockServerItem.port()}", TestDto())
        wiremockServerItem.verify(
            postRequestedFor(WireMock.anyUrl())
                .withRequestBody(equalToJson("""{"dato" : "2020-01-01"} """))
        )
    }

    @Test
    internal fun `default webClient skal sende datoer som iso`() {
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.ok()))
        val build = familieWebClientBuilder.build()
        val response = build
            .post()
            .uri("http://localhost:${wiremockServerItem.port()}")
            .bodyValue(TestDto())
            .retrieve()
            .bodyToMono<String>()
            .block()

        wiremockServerItem.verify(
            postRequestedFor(WireMock.anyUrl())
                .withRequestBody(equalToJson("""{"dato" : "2020-01-01"} """))
        )
    }

    @Test
    internal fun `default webClient skal kunne ta emot stor request`() {
        val fil = this::class.java.classLoader.getResource("dummy/image.jpg").readText()
        val mockResponse = WireMock.okJson(fil)
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(mockResponse))
        val build = familieWebClientBuilder.build()

        val response = build
            .post()
            .uri("http://localhost:${wiremockServerItem.port()}")
            .bodyValue(TestDto())
            .retrieve()
            .bodyToMono<String>()
            .block()
    }

    companion object {

        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
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
}
