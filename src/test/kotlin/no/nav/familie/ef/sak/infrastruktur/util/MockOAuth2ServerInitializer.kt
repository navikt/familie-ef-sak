package no.nav.familie.ef.sak.infrastruktur.util

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

class MockOAuth2ServerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val mockOAuth2Server = MockOAuth2ServerHolder.server
        val azureIssuerUrl = mockOAuth2Server.issuerUrl("azuread").toString()
        val tokenxIssuerUrl = mockOAuth2Server.issuerUrl("tokenx").toString()
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "AZURE_OPENID_CONFIG_ISSUER=$azureIssuerUrl",
            "AZURE_APP_CLIENT_ID=aud-localhost",
            "TOKEN_X_ISSUER=$tokenxIssuerUrl",
        )
    }

    companion object {
        val server: MockOAuth2Server get() = MockOAuth2ServerHolder.server
    }
}

object MockOAuth2ServerHolder {
    val server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { it.start() }
    }
}
