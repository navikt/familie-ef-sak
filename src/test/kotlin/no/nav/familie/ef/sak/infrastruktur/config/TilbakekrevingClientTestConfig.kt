package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingClient
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
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

        val dummyPdf = this::class.java.classLoader.getResource("dummy/pdf_dummy.pdf")!!.readBytes()
        every { tilbakekrevingClient.hentForhåndsvisningVarselbrev(any()) } returns dummyPdf

        every { tilbakekrevingClient.opprettManuelTilbakekreving(any(), any(), any()) } just runs
        val kanBehandleRespons = KanBehandlingOpprettesManueltRespons(kanBehandlingOpprettes = true, melding = "OK")
        every { tilbakekrevingClient.kanBehandlingOpprettesManuelt(any(), any()) } returns kanBehandleRespons

        return tilbakekrevingClient
    }
}
