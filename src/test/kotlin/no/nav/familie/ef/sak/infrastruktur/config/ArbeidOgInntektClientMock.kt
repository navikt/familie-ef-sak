package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.amelding.ekstern.ArbeidOgInntektClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class ArbeidOgInntektClientMock {
    @Profile("mock-arbeid-og-inntekt")
    @Bean
    @Primary
    fun arbeidOgInntekt(): ArbeidOgInntektClient {
        val mockk = mockk<ArbeidOgInntektClient>()

        every { mockk.genererAInntektUrl(any()) } returns "https://ainntekt"
        return mockk
    }
}
