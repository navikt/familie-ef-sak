package no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.blankett.BlankettClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-blankett")
class BlankettClientMock {

    @Bean
    @Primary
    fun blankettClient(): BlankettClient {
        val mock = mockk<BlankettClient>()
        val dummyPdf = this::class.java.classLoader.getResource("dummy/pdf_dummy.pdf").readBytes()
        every { mock.genererBlankett(any()) } returns dummyPdf
        return mock
    }

}