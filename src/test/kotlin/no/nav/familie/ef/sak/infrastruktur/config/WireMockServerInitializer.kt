package no.nav.familie.ef.sak.infrastruktur.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

class WireMockServerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "FAMILIE_INTEGRASJONER_URL=http://localhost:${wireMockServer.port()}",
        )
    }

    companion object {
        val wireMockServer: WireMockServer by lazy {
            WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()).also { it.start() }
        }
    }
}
