package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_SIGNATUR_PLACEHOLDER
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class BrevClientMock {
    @Profile("mock-brev")
    @Bean
    @Primary
    fun brevClient(): BrevClient {
        val brevClient = mockk<BrevClient>()
        every { brevClient.genererHtml(any(), any(), any(), any(), any()) } returns "<h1>Hei $BESLUTTER_SIGNATUR_PLACEHOLDER</h1>"
        val dummyPdf =
            this::class.java.classLoader
                .getResource("dummy/pdf_dummy.pdf")
                .readBytes()
        every { brevClient.genererBlankett(any()) } returns dummyPdf
        return brevClient
    }
}
