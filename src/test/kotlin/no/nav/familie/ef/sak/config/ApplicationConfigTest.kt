package no.nav.familie.ef.sak.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.http.sts.StsRestClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.getForEntity
import org.springframework.web.client.postForEntity
import java.time.LocalDate

@Disabled
internal class ApplicationConfigTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var restTemplateBuilder: RestTemplateBuilder

    data class TestDto(val dato: LocalDate = LocalDate.of(2020, 1, 1))

    @Test
    internal fun `default restTemplateBuilder skal sende datoer som array`() {
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.ok()))
        val restTemplate = restTemplateBuilder.build()
        restTemplate.postForEntity<String>("http://localhost:${wiremockServerItem.port()}", TestDto())
        wiremockServerItem.verify(postRequestedFor(WireMock.anyUrl())
                                          .withRequestBody(equalToJson("""{"dato" : "2020-01-01"} """))
        )
    }

    @Test
    internal fun `restTemplate skal ikke bruke customizer med proxy for microsoft`() {
        // skal ikke bruke felles sin restTemplate som går via webproxy for hostnames med microsoft
        // Connect to webproxy-nais.nav.no:8088
        val build = restTemplateBuilder.build()
        val customizers = RestTemplateBuilder::class.java.getDeclaredField("customizers")
        customizers.isAccessible = true
        assertThat(customizers.get(restTemplateBuilder) as Set<Any>).isEmpty()
        assertThat(catchThrowable { build.getForEntity<String>("http://microsoft") })
                .hasMessageContaining("I/O error on GET request for \"http://microsoft\": microsoft: nodename nor servname provided")
    }

    companion object {

        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            val stsRestClient = mockk<StsRestClient>()
            every { stsRestClient.systemOIDCToken } returns "token"

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