package no.nav.familie.ef.sak.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@ActiveProfiles("local", "mock-oauth", "mock-integrasjoner")
internal class KodeverkServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var kodeverkService: KodeverkService
    @Autowired lateinit var familieIntegrasjonerConfig: IntegrasjonerConfig
    @Autowired lateinit var wireMockServer: WireMockServer

    @Test
    internal fun `skal cachea poststed og land`() {
        kodeverkService.hentPoststed("0575", LocalDate.now())
        kodeverkService.hentPoststed("0576", LocalDate.now())

        kodeverkService.hentLand("NOR", LocalDate.now())
        kodeverkService.hentLand("SWE", LocalDate.now())

        wireMockServer.verify(1, getRequestedFor(urlEqualTo(familieIntegrasjonerConfig.kodeverkPoststedUri.path)))
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(familieIntegrasjonerConfig.kodeverkLandkoderUri.path)))
    }

}
