package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingClient
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDateTime
import java.util.UUID

@TestConfiguration
class TilbakekrevingClientTestConfig {

    @Bean
    @Profile("mock-tilbakekreving")
    @Primary
    fun mockTilbakekrevingKlient(): TilbakekrevingClient {
        val tilbakekrevingClient: TilbakekrevingClient = mockk()

        every { tilbakekrevingClient.hentForhåndsvisningVarselbrev(any()) } returns pdfAsBase64String.toByteArray()

        every { tilbakekrevingClient.finnesÅpenBehandling(any()) } returns false

        every { tilbakekrevingClient.finnBehandlinger(any()) } returns listOf(Behandling(behandlingId = UUID.randomUUID(),
                                                                                         opprettetTidspunkt = LocalDateTime.now(),
                                                                                         aktiv = true,
                                                                                         årsak = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                                                                                         type = Behandlingstype.TILBAKEKREVING,
                                                                                         status = Behandlingsstatus.OPPRETTET,
                                                                                         vedtaksdato = null,
                                                                                         resultat = null))

        return tilbakekrevingClient
    }
}
