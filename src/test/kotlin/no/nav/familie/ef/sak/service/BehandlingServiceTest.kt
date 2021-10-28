package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.BEHANDLES_I_GOSYS
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.FEILREGISTRERT
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

internal class BehandlingServiceTest {

    private val behandlingRepository = mockk<BehandlingRepository>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = BehandlingService(mockk(),
                                                      behandlingRepository,
                                                      behandlingshistorikkService,
                                                      mockk(),
                                                      oppgaveService)

    @Test
    internal fun `skal henlegge behandling som er blankett og status utredes`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.UTREDES)
        henleggOgForventOk(behandling, BEHANDLES_I_GOSYS)
    }

    @Test
    internal fun `skal henlegge behandling som er blankett og status opprettet`() {
        val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.OPPRETTET)
        henleggOgForventOk(behandling, BEHANDLES_I_GOSYS)
    }

    @Test
    internal fun `skal ikke kunne henlegge behandling hvor vedtak fattes`() {
        val behandling =
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FATTER_VEDTAK)
        henleggOgForventFeilmelding(behandling, FEILREGISTRERT)
    }

    @Test
    internal fun `skal ikke kunne henlegge behandling som er iverksatt`() {
        val behandling =
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.IVERKSETTER_VEDTAK)
        henleggOgForventFeilmelding(behandling, TRUKKET_TILBAKE)
    }

    @Test
    internal fun `skal ikke kunne henlegge behandling som er ferdigstilt`() {
        val behandling = behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FERDIGSTILT)
        henleggOgForventFeilmelding(behandling, TRUKKET_TILBAKE)
    }

    @Test
    internal fun `skal kunne henlegge behandling som er førstegangsbehandling`() {
        val behandling = behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.UTREDES)
        henleggOgForventOk(behandling, henlagtÅrsak = FEILREGISTRERT)
    }

    @Test
    internal fun `skal kunne henlegge behandling som er revurdering`() {
        val behandling = behandling(fagsak(), type = BehandlingType.REVURDERING, status = BehandlingStatus.UTREDES)
        henleggOgForventOk(behandling, FEILREGISTRERT)
    }

    private fun henleggOgForventOk(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
        every {
            behandlingRepository.findByIdOrThrow(any())
        } returns behandling

        every {
            oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(any(), any())
        } just runs

        every {
            behandlingshistorikkService.opprettHistorikkInnslag(any(), any(), any())
        } just runs

        val behandlingSlot = slot<Behandling>()
        every {
            behandlingRepository.update(capture(behandlingSlot))
        } answers {
            behandlingSlot.captured
        }
        behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
        assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(behandlingSlot.captured.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(behandlingSlot.captured.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }


    private fun henleggOgForventFeilmelding(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
        every {
            behandlingRepository.findByIdOrThrow(any())
        } returns behandling

        every {
            behandlingshistorikkService.opprettHistorikkInnslag(any<Behandling>())
        } just runs

        val feil: Feil = assertThrows {
            behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
        }

        assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}