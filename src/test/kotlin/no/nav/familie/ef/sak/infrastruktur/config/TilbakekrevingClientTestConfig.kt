package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@TestConfiguration
class TilbakekrevingClientTestConfig {

    @Bean
    @Profile("mock-tilbakekreving")
    @Primary
    fun mockTilbakekrevingKlient(): TilbakekrevingClient {
        val tilbakekrevingClient: TilbakekrevingClient = mockk()

        every { tilbakekrevingClient.hentForhåndsvisningVarselbrev(any()) } returns pdfAsBase64String.toByteArray()

        every { tilbakekrevingClient.finnesÅpenBehandling(any()) } returns false

        every { tilbakekrevingClient.finnBehandlinger(any()) } returns emptyList()

        return tilbakekrevingClient
    }
}
