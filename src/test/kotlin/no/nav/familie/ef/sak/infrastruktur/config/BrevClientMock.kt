package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.BrevClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-brev")
class BrevClientMock {

    @Bean
    @Primary
    fun brevClient(): BrevClient {
        val brevClient: BrevClient = mockk()
        val dummyPdf = this::class.java.classLoader.getResource("dummy/pdf_dummy.pdf").readBytes()
        every { brevClient.genererBrev(any()) } returns dummyPdf
        return brevClient
    }
}