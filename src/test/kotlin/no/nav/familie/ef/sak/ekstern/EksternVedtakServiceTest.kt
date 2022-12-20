package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.EksternFagsakId
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingClient
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class EksternVedtakServiceTest {

    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilbakekrevingClient = mockk<TilbakekrevingClient>()

    private val service = EksternVedtakService(
        fagsakService = fagsakService,
        behandlingService = behandlingService,
        tilbakekrevingClient = tilbakekrevingClient
    )

    private val eksternFagsakId: Long = 10
    private val fagsak = fagsak(eksternId = EksternFagsakId(eksternFagsakId))

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakPåEksternId(eksternFagsakId) } returns fagsak
        every { tilbakekrevingClient.finnVedtak(eksternFagsakId) } returns emptyList()
    }

    @Test
    internal fun `skal mappe ferdigstilte behandlinger til fagsystemVedtak`() {
        val vedtakstidspunkt = LocalDateTime.now()
        val behandling = ferdigstiltBehandling(vedtakstidspunkt)
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling)

        val vedtak = service.hentVedtak(eksternFagsakId)
        assertThat(vedtak).hasSize(1)
        assertThat(vedtak[0].resultat).isEqualTo("Avslått")
        assertThat(vedtak[0].behandlingstype).isEqualTo("Førstegangsbehandling")
        assertThat(vedtak[0].eksternBehandlingId).isEqualTo(behandling.eksternId.id.toString())
        assertThat(vedtak[0].vedtakstidspunkt).isEqualTo(vedtakstidspunkt)
        assertThat(vedtak[0].fagsystemType).isEqualTo(FagsystemType.ORDNIÆR)
    }

    @Test
    internal fun `skal returnere behandlinger fra fagsystem og tilbakekreving`() {
        val vedtakstidspunkt = LocalDateTime.now()
        val behandling = ferdigstiltBehandling(vedtakstidspunkt)
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling)
        every { tilbakekrevingClient.finnVedtak(eksternFagsakId) } returns listOf(fagsystemVedtakTilbakekreving())

        val vedtak = service.hentVedtak(eksternFagsakId)
        assertThat(vedtak.map { it.fagsystemType }).containsExactly(FagsystemType.ORDNIÆR, FagsystemType.TILBAKEKREVING)

        verify(exactly = 1) { behandlingService.hentBehandlinger(fagsak.id) }
        verify(exactly = 1) { tilbakekrevingClient.finnVedtak(eksternFagsakId) }
    }

    @Test
    internal fun `dersom behandlingstypen er sanksjon skal dette mappes til fagsystemType`() {
        val vedtakstidspunkt = LocalDateTime.now()
        val behandling = ferdigstiltBehandling(vedtakstidspunkt).copy(årsak = BehandlingÅrsak.SANKSJON_1_MND)
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling)

        val vedtak = service.hentVedtak(eksternFagsakId)
        assertThat(vedtak.map { it.fagsystemType }).containsExactly(FagsystemType.SANKSJON_1_MND)
        assertThat(vedtak).hasSize(1)
        assertThat(vedtak.first().resultat).isEqualTo("Sanksjon 1 måned")

        verify(exactly = 1) { behandlingService.hentBehandlinger(fagsak.id) }
    }

    @Test
    internal fun `skal ikke returnere henlagte behandlinger`() {
        val henlagtBehandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.REVURDERING,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.HENLAGT
        )
        every { behandlingService.hentBehandlinger(any<UUID>()) } returns listOf(henlagtBehandling)

        assertThat(service.hentVedtak(eksternFagsakId)).isEmpty()
    }

    @Test
    internal fun `skal ikke returnere behandlinger under behandling`() {
        val henlagtBehandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.REVURDERING,
            status = BehandlingStatus.UTREDES,
            resultat = BehandlingResultat.IKKE_SATT
        )
        every { behandlingService.hentBehandlinger(any<UUID>()) } returns listOf(henlagtBehandling)

        assertThat(service.hentVedtak(eksternFagsakId)).isEmpty()
    }

    private fun ferdigstiltBehandling(vedtakstidspunkt: LocalDateTime?): Behandling = behandling(
        fagsak,
        vedtakstidspunkt = vedtakstidspunkt,
        resultat = BehandlingResultat.AVSLÅTT,
        type = BehandlingType.FØRSTEGANGSBEHANDLING,
        status = BehandlingStatus.FERDIGSTILT
    )

    private fun fagsystemVedtakTilbakekreving() = FagsystemVedtak(
        eksternBehandlingId = UUID.randomUUID().toString(),
        behandlingstype = "Tilbakekreving",
        resultat = "Delvis tilbakebetaling",
        vedtakstidspunkt = LocalDateTime.now(),
        fagsystemType = FagsystemType.TILBAKEKREVING,
    )
}
