package no.nav.familie.ef.sak.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.ef.infotrygd.OpprettPeriodeHendelseDto
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.kontrakter.ef.infotrygd.OpprettVedtakHendelseDto
import no.nav.familie.kontrakter.ef.infotrygd.StønadType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

internal class InfotrygdFeedClientTest {

    companion object {

        private val restOperations: RestOperations = RestTemplateBuilder().build()
        lateinit var client: InfotrygdFeedClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            val stsRestClient = mockk<StsRestClient>()
            every { stsRestClient.systemOIDCToken } returns "token"
            client = InfotrygdFeedClient(URI.create(wiremockServerItem.baseUrl()), restOperations)

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
    internal fun `opprettPeriodeHendelse`() {
        wiremockServerItem.stubFor(
                WireMock.post(WireMock.urlPathMatching(client.opprettPeriodeUri.path))
                        .willReturn(WireMock.ok()))
        client.opprettPeriodeHendelse(OpprettPeriodeHendelseDto("", StønadType.BARNETILSYN, emptyList()))
    }

    @Test
    internal fun `opprettStartBehandlingHendelse`() {
        wiremockServerItem.stubFor(
                WireMock.post(WireMock.urlPathMatching(client.opprettStartBehandlingUri.path))
                        .willReturn(WireMock.ok()))
        client.opprettStartBehandlingHendelse(OpprettStartBehandlingHendelseDto("", StønadType.BARNETILSYN))
    }

    @Test
    internal fun `opprettVedtakHendelse`() {
        wiremockServerItem.stubFor(
                WireMock.post(WireMock.urlPathMatching(client.opprettVedtakUri.path))
                        .willReturn(WireMock.ok()))
        client.opprettVedtakHendelse(OpprettVedtakHendelseDto("", StønadType.BARNETILSYN, LocalDate.now()))
    }
}