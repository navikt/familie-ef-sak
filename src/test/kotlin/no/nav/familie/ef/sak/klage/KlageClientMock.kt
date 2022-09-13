package no.nav.familie.ef.sak.no.nav.familie.ef.sak.klage

import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.ef.sak.klage.KlageClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class KlageClientMock {

    @Profile("mock-klage")
    @Bean
    @Primary
    fun klageClient(): KlageClient {
        val mockk = mockk<KlageClient>()
        justRun { mockk.opprettKlage(any()) }
        return mockk
    }
}
