package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.Vedtaksbrev.BrevClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-brev")
class BrevClientMock {


    @Bean
    @Primary
    fun brevClien(): BrevClient {
        val brevClient: BrevClient = mockk()

        val pdf = ByteArray(123)

        every { brevClient.genererBrev(any(), any(), any()) } returns pdf

        return brevClient
    }
}