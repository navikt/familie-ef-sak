package no.nav.familie.ef.sak.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local", "mock-auth", "mock-oauth", "mock-integrasjoner")
internal class KodeverkServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var kodeverkService: KodeverkService
    @Autowired lateinit var familieIntegrasjonerConfig: IntegrasjonerConfig
    @Autowired lateinit var wireMockServer: WireMockServer

    @Test
    internal fun `skal cachea poststed`() {
        kodeverkService.hentPoststed()
        kodeverkService.hentPoststed()
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(familieIntegrasjonerConfig.kodeverkPoststedUri.path)))
    }

}
