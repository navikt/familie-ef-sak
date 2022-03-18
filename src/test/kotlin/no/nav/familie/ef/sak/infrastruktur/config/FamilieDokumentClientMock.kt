package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-dokument")
class FamilieDokumentClientMock {

    @Bean
    @Primary
    fun familieDokument(): FamilieDokumentClient {
        val familieDokumentClient: FamilieDokumentClient = mockk()
        val dummyPdf = this::class.java.classLoader.getResource("dummy/pdf_dummy.pdf")!!.readBytes()
        every { familieDokumentClient.genererPdfFraHtml(any()) } returns dummyPdf
        return  familieDokumentClient
    }
}