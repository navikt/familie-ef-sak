package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.MellomlagringBrevService
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NullstillVedtakService(
    private val vedtakService: VedtakService,
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val mellomlagringBrevService: MellomlagringBrevService,
    private val vedtaksbrevService: VedtaksbrevService,
    private val tilordnetRessursService: TilordnetRessursService,
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) {
    @Transactional
    fun nullstillVedtak(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandling er låst og vedtak kan ikke slettes"
        }
        feilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)) {
            "Behandlingen har en annen eier og vedtak kan derfor ikke slettes av deg"
        }

        mellomlagringBrevService.slettMellomlagringHvisFinnes(behandlingId)
        simuleringService.slettSimuleringForBehandling(saksbehandling)
        tilkjentYtelseService.slettTilkjentYtelseForBehandling(behandlingId)
        tilbakekrevingService.slettTilbakekreving(behandlingId)
        if (saksbehandling.steg.kommerEtter(StegType.BEREGNE_YTELSE)) {
            stegService.resetSteg(behandlingId, StegType.BEREGNE_YTELSE)
        }
        vedtakService.slettVedtakHvisFinnes(behandlingId)
        vedtaksbrevService.slettVedtaksbrev(saksbehandling)
        oppfølgingsoppgaveService.slettOppfølgingsoppgave(behandlingId)
    }
}
