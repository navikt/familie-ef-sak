package no.nav.familie.ef.sak.infrastruktur.config

import no.nav.familie.ef.sak.infrastruktur.util.MockOAuth2ServerHolder
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockOAuth2ServerConfig {
    @Bean
    fun mockOAuth2Server(): MockOAuth2Server = MockOAuth2ServerHolder.server
}
