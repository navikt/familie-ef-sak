package no.nav.familie.ef.sak.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.web.client.postForEntity
import java.time.LocalDate

@Disabled
internal class ApplicationConfigTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var restTemplateBuilder: RestTemplateBuilder

    data class TestDto(
        val dato: LocalDate = LocalDate.of(2020, 1, 1),
    )

    @Test
    internal fun `default restTemplateBuilder skal sende datoer som iso`() {
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.ok()))
        val restTemplate = restTemplateBuilder.build()
        restTemplate.postForEntity<String>("http://localhost:${wiremockServerItem.port()}", TestDto())
        wiremockServerItem.verify(
            postRequestedFor(WireMock.anyUrl())
                .withRequestBody(equalToJson("""{"dato" : "2020-01-01"} """)),
        )
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
