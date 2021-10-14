package no.nav.familie.ef.sak.tilbakekreving

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class TilbakekrevingServiceTest {


    private val tilbakekrevingRepository = mockk<TilbakekrevingRepository>()
    val behandlingService = mockk<BehandlingService>()
    private val tilbakekrevingService = TilbakekrevingService(tilbakekrevingRepository, behandlingService)

    @Test
    internal fun `skal ikke være mulig å lagre tilbakekreving for låst behandlig `() {
        every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak(), status = FERDIGSTILT)
        val tilbakekrevingDto =
                TilbakekrevingDto(valg = Tilbakekrevingsvalg.AVVENT, varseltekst = "", begrunnelse = "Dette er tekst ")
        val feil = assertThrows<Feil> {
            tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto,
                                                      behandlingId = UUID.randomUUID())
        }
        assertThat(feil.message).isEqualTo("Behandlingen er låst for redigering")
    }

    @Test
    internal fun `Skal kaste feil dersom vi forsøker lagre tilbakekreving med varsl som mangler varseltekst`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak())
        val tilbakekrevingDto =
                TilbakekrevingDto(valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL, varseltekst = null, begrunnelse = "tekst her")

        val feil = assertThrows<Feil> {
            tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto,
                                                      behandlingId = UUID.randomUUID())
        }
        assertThat(feil.message).isEqualTo("Må fylle ut varseltekst for å lage tilbakekreving med varsel")
    }


    @Test
    internal fun `Skal kaste feil dersom vi forsøker lagre tilbakekreving med varsel som har tom varseltekst`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak())
        val tilbakekrevingDto =
                TilbakekrevingDto(valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL, varseltekst = "   ", begrunnelse = "tekst her")

        val feil = assertThrows<Feil> {
            tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto,
                                                      behandlingId = UUID.randomUUID())
        }
        assertThat(feil.message).isEqualTo("Må fylle ut varseltekst for å lage tilbakekreving med varsel")
    }

    @Test
    internal fun `Skal lagre forventet tilbakekreving entitet når alt går bra`() {
        every { behandlingService.hentBehandling(any()) } returns behandling(fagsak = fagsak())
        val tilbakekrevingSlot = slot<Tilbakekreving>()
        val forventetBegrunnelse = "tekst her"
        val varseltekst = "Dette er en varseltekst"
        val tilbakekrevingDto =
                TilbakekrevingDto(valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                                  varseltekst = varseltekst,
                                  begrunnelse = forventetBegrunnelse)

        val behandlingId = UUID.randomUUID()
        every { tilbakekrevingRepository.deleteById(any()) } just Runs
        every { tilbakekrevingRepository.insert(capture(tilbakekrevingSlot)) } answers { firstArg() }
        tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto, behandlingId = behandlingId)
        assertThat(tilbakekrevingSlot.captured.behandlingId).isEqualTo(behandlingId)
        assertThat(tilbakekrevingSlot.captured.begrunnelse).isEqualTo(forventetBegrunnelse)
    }

}

