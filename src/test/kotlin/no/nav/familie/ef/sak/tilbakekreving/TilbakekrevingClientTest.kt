package no.nav.familie.ef.sak.tilbakekreving

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriUtils
import java.net.URI
import java.nio.charset.Charset

internal class TilbakekrevingClientTest {
    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }

    @Test
    internal fun `skal håndtere Ø i url`() {
        val encodedStønadstype = UriUtils.encodePath(StønadType.OVERGANGSSTØNAD.name, Charset.forName("UTF-8"))
        val eksternFagsakId = 1L
        val jsonResponse = jsonMapper.writeValueAsString(success(KanBehandlingOpprettesManueltRespons(true, "")))
        val url =
            "/api/ytelsestype/$encodedStønadstype/fagsak/$eksternFagsakId/kanBehandlingOpprettesManuelt/v1"
        wiremockServerItem.stubFor(
            WireMock
                .get(WireMock.urlEqualTo(url))
                .willReturn(WireMock.okJson(jsonResponse)),
        )
        val kanBehandlingOpprettesManuelt =
            client.kanBehandlingOpprettesManuelt(StønadType.OVERGANGSSTØNAD, eksternFagsakId)

        assertThat(kanBehandlingOpprettesManuelt.kanBehandlingOpprettes).isTrue
    }

    companion object {
        private val restOperations: RestOperations =
            RestTemplateBuilder()
                .additionalMessageConverters(
                    JacksonJsonHttpMessageConverter(jsonMapper),
                ).build()
        lateinit var client: TilbakekrevingClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            client = TilbakekrevingClient(restOperations, URI.create(wiremockServerItem.baseUrl()))
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }
}
