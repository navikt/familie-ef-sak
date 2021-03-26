package no.nav.familie.ef.sak.service

import io.mockk.*
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.ManglerTilgang
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.steg.StegType
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

internal class BehandlingServiceTest {

    val behandlingRepository = mockk<BehandlingRepository>()
    val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    val behandlingService = BehandlingService(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), behandlingRepository, behandlingshistorikkService)

    @Test
    internal fun `skal annullere behandling som er blankett og status utredes`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.UTREDES)
        annullerOgForventOk(behandling)
    }

    @Test
    internal fun `skal annullere behandling som er blankett og status opprettet`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.OPPRETTET)
        annullerOgForventOk(behandling)
    }

    @Test
    internal fun `skal ikke kunne annullere behandling hvor vedtak fattes`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.FATTER_VEDTAK)
        annullerOgForventFeilmelding(behandling)
    }

    @Test
    internal fun `skal ikke kunne annullere behandling som er iverksatt`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.IVERKSETTER_VEDTAK)
        annullerOgForventFeilmelding(behandling)
    }

    @Test
    internal fun `skal ikke kunne annullere behandling som er ferdigstilt`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.FERDIGSTILT)
        annullerOgForventFeilmelding(behandling)
    }


    @Test
    internal fun `skal ikke kunne annullere behandling som er førstegangsbehandling`() {
        val behandling = behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.UTREDES)
        annullerOgForventFeilmelding(behandling)
    }

    @Test
    internal fun `skal ikke kunne annullere behandling som er revurdering`() {
        val behandling = behandling(fagsak(), type = BehandlingType.REVURDERING, status = BehandlingStatus.UTREDES)
        annullerOgForventFeilmelding(behandling)
    }


    private fun annullerOgForventOk(behandling: Behandling) {
        every {
            behandlingRepository.findByIdOrThrow(any())
        } returns behandling

        every {
            behandlingshistorikkService.opprettHistorikkInnslag(any<Behandling>())
        } just runs

        val behandlingSlot = slot<Behandling>()
        every {
            behandlingRepository.update(capture(behandlingSlot))
        } answers {
            behandlingSlot.captured
        }
        behandlingService.annullerBehandling(behandling.id)

        assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(behandlingSlot.captured.resultat).isEqualTo(BehandlingResultat.ANNULLERT)
        assertThat(behandlingSlot.captured.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }


    private fun annullerOgForventFeilmelding(behandling: Behandling) {
        every {
            behandlingRepository.findByIdOrThrow(any())
        } returns behandling

        every {
            behandlingshistorikkService.opprettHistorikkInnslag(any<Behandling>())
        } just runs

        val feil: Feil = assertThrows<Feil> {
            behandlingService.annullerBehandling(behandling.id)
        }

        assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}