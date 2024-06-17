package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingClient
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
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

        every { tilbakekrevingClient.hentForhåndsvisningVarselbrev(any()) } returns PDF_AS_BASE64_STRING.toByteArray()

        every { tilbakekrevingClient.finnesÅpenBehandling(any()) } returns false

        every { tilbakekrevingClient.finnBehandlinger(any()) } returns
            listOf(
                Behandling(
                    behandlingId = UUID.randomUUID(),
                    opprettetTidspunkt = LocalDateTime.now(),
                    aktiv = true,
                    årsak = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                    type = Behandlingstype.TILBAKEKREVING,
                    status = Behandlingsstatus.OPPRETTET,
                    vedtaksdato = null,
                    resultat = null,
                ),
            )

        every { tilbakekrevingClient.finnVedtak(any()) } returns
            listOf(
                FagsystemVedtak(
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    behandlingstype = "Tilbakekreving",
                    resultat = "Delvis tilbakebetaling",
                    vedtakstidspunkt = LocalDateTime.now(),
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                    regelverk = Regelverk.NASJONAL,
                ),
            )

        val dummyPdf =
            this::class.java.classLoader
                .getResource("dummy/pdf_dummy.pdf")!!
                .readBytes()
        every { tilbakekrevingClient.hentForhåndsvisningVarselbrev(any()) } returns dummyPdf

        every { tilbakekrevingClient.opprettManuellTilbakekreving(any(), any(), any()) } returns Ressurs.success("OK")
        val kanBehandleRespons = KanBehandlingOpprettesManueltRespons(kanBehandlingOpprettes = true, melding = "OK")
        every { tilbakekrevingClient.kanBehandlingOpprettesManuelt(any(), any()) } returns kanBehandleRespons

        return tilbakekrevingClient
    }
}
