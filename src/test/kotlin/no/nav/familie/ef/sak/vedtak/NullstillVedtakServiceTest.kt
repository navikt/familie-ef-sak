package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.MellomlagringBrevService
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.NullstillVedtakService
import no.nav.familie.ef.sak.vedtak.VedtakService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class NullstillVedtakServiceTest {
    private val vedtakService = mockk<VedtakService>(relaxed = true)

    private val stegService = mockk<StegService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val mellomlagringBrevService = mockk<MellomlagringBrevService>(relaxed = true)
    private val vedtaksbrevService = mockk<VedtaksbrevService>(relaxed = true)
    private val oppfølgingsoppgaveService = mockk<OppfølgingsoppgaveService>(relaxed = true)
    private val tilordnetRessursService = mockk<TilordnetRessursService>(relaxed = true)

    private val nullstillVedtakService =
        NullstillVedtakService(
            vedtakService,
            stegService,
            behandlingService,
            simuleringService,
            tilkjentYtelseService,
            tilbakekrevingService,
            mellomlagringBrevService,
            vedtaksbrevService,
            tilordnetRessursService,
            oppfølgingsoppgaveService,
        )
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
    }

    @Test
    fun `nullstill vedtak`() {
        val saksbehandling = slot<Saksbehandling>()
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            saksbehandling(
                id = behandlingId,
                steg = StegType.BEHANDLING_FERDIGSTILT,
            )

        nullstillVedtakService.nullstillVedtak(behandlingId)

        verifyAll {
            mellomlagringBrevService.slettMellomlagringHvisFinnes(behandlingId)
            simuleringService.slettSimuleringForBehandling(capture(saksbehandling))
            tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandlingId)
            tilbakekrevingService.slettTilbakekreving(behandlingId)
            vedtaksbrevService.slettVedtaksbrev(any())
            vedtakService.slettVedtakHvisFinnes(behandlingId)
            stegService.resetSteg(behandlingId, StegType.BEREGNE_YTELSE)
            oppfølgingsoppgaveService.slettOppfølgingsoppgave(any())
        }

        Assertions.assertThat(saksbehandling.captured.id).isEqualTo(behandlingId)
    }

    @Test
    internal fun `skal ikke resette steg hvis steget ikke er etter vedtak`() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            saksbehandling(
                id = behandlingId,
                steg = StegType.VILKÅR,
            )
        nullstillVedtakService.nullstillVedtak(behandlingId)
        verify(exactly = 0) {
            stegService.resetSteg(any(), any())
        }
        verify {
            vedtakService.slettVedtakHvisFinnes(behandlingId)
        }
    }

    @Test
    fun `nullstill vedtak skal feile når behandling er ferdigstilt`() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            saksbehandling(
                id = behandlingId,
                status = BehandlingStatus.FERDIGSTILT,
            )

        assertThrows<Feil> { nullstillVedtakService.nullstillVedtak(behandlingId) }
    }

    @Test
    fun `nullstill vedtak skal feile når behandling er sendt til beslutter`() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            saksbehandling(
                id = behandlingId,
                status = BehandlingStatus.FATTER_VEDTAK,
            )

        assertThrows<Feil> { nullstillVedtakService.nullstillVedtak(behandlingId) }
    }

    @Test
    fun `nullstill vedtak skal feile når behanlding iverksettes`() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            saksbehandling(
                id = behandlingId,
                status = BehandlingStatus.IVERKSETTER_VEDTAK,
            )

        assertThrows<Feil> { nullstillVedtakService.nullstillVedtak(behandlingId) }
    }
}
