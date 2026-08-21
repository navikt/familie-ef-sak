package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.mockk
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class SigrunClientMock {
    @Profile("mock-sigrun")
    @Bean
    @Primary
    fun sigrunClient(): SigrunClient = mockk<SigrunClient>()
}
