package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.domain.VedtaksbrevKonstanter.IKKE_SATT_IDENT_PÅ_GAMLE_VEDTAKSBREV
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType.INNVILGE
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SendTilBeslutterSteg(private val taskRepository: TaskRepository,
                           private val oppgaveService: OppgaveService,
                           private val fagsakService: FagsakService,
                           private val behandlingService: BehandlingService,
                           private val vedtaksbrevRepository: VedtaksbrevRepository,
                           private val vedtakService: VedtakService,
                           private val simuleringService: SimuleringService,
                           private val tilbakekrevingService: TilbakekrevingService,
                           private val vurderingService: VurderingService) : BehandlingSteg<Void?> {

    override fun validerSteg(saksbehandling: Saksbehandling) {
        if (saksbehandling.steg != stegType()) {
            throw ApiFeil("Behandling er i feil steg=${saksbehandling.steg}", HttpStatus.BAD_REQUEST)
        }

        if (saksbehandling.type !== BehandlingType.BLANKETT && !vedtaksbrevRepository.existsById(saksbehandling.id)) {
            throw Feil("Brev mangler for behandling=${saksbehandling.id}")
        }
        brukerfeilHvis(saksbehandlerMåTaStilingTilTilbakekreving(saksbehandling)) {
            "Feilutbetaling detektert. Må ta stilling til feilutbetalingsvarsel under simulering"
        }
        validerRiktigTilstandVedInvilgelse(saksbehandling)
        validerSaksbehandlersignatur(saksbehandling)

    }

    private fun validerRiktigTilstandVedInvilgelse(behandling: Saksbehandling) {
        val vedtak = vedtakService.hentVedtak(behandling.id)
        if (vedtak.resultatType == INNVILGE) {
            brukerfeilHvisIkke(vurderingService.erAlleVilkårOppfylt(behandling.id)) {
                "Kan ikke innvilge hvis ikke alle vilkår er oppfylt for behandlingId: ${behandling.id}"
            }
        }
    }

    private fun saksbehandlerMåTaStilingTilTilbakekreving(behandling: Saksbehandling): Boolean {
        if (erIkkeRelevantForTilbakekreving(behandling)) {
            return false
        }
        val feilutbetaling = simuleringService.hentLagretSimuleringsresultat(behandling.id).feilutbetaling > BigDecimal.ZERO
        val harIkkeTattStillingTil = !tilbakekrevingService.harSaksbehandlerTattStillingTilTilbakekreving(behandling.id)
        if (feilutbetaling && harIkkeTattStillingTil) {
            return !tilbakekrevingService.finnesÅpenTilbakekrevingsBehandling(behandling.id)
        }

        return false
    }

    private fun erIkkeRelevantForTilbakekreving(behandling: Saksbehandling): Boolean {
        val resultatType = vedtakService.hentVedtak(behandling.id).resultatType
        return behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING || behandling.type == BehandlingType.BLANKETT || resultatType == ResultatType.AVSLÅ || resultatType == ResultatType.HENLEGGE
    }

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.FATTER_VEDTAK)
        vedtakService.oppdaterSaksbehandler(saksbehandling.id, SikkerhetContext.hentSaksbehandler(strict = true))
        opprettGodkjennVedtakOppgave(saksbehandling)
        ferdigstillOppgave(saksbehandling, Oppgavetype.BehandleSak)
        ferdigstillOppgave(saksbehandling, Oppgavetype.BehandleUnderkjentVedtak)
        opprettTaskForBehandlingsstatistikk(saksbehandling.id)
    }


    private fun opprettTaskForBehandlingsstatistikk(behandlingId: UUID) =
            taskRepository.save(BehandlingsstatistikkTask.opprettVedtattTask(behandlingId = behandlingId))

    private fun ferdigstillOppgave(behandling: Saksbehandling, oppgavetype: Oppgavetype) {
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id,
                                                                   oppgavetype,
                                                                   it.gsakOppgaveId,
                                                                   aktivIdent))
        }
    }


    private fun opprettGodkjennVedtakOppgave(behandling: Saksbehandling) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = behandling.id,
                                       oppgavetype = Oppgavetype.GodkjenneVedtak,
                                       beskrivelse = "Sendt til godkjenning av ${SikkerhetContext.hentSaksbehandlerNavn(true)}.")))
    }

    private fun validerSaksbehandlersignatur(behandling: Saksbehandling) {
        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(behandling.id)

        when (vedtaksbrev.saksbehandlerident) {
            IKKE_SATT_IDENT_PÅ_GAMLE_VEDTAKSBREV -> validerSammeSignatur(vedtaksbrev)
            else -> validerSammeIdent(vedtaksbrev)
        }

    }

    private fun validerSammeIdent(vedtaksbrev: Vedtaksbrev) {
        brukerfeilHvis(vedtaksbrev.saksbehandlerident != SikkerhetContext.hentSaksbehandler(true)) { "En annen saksbehandler har signert vedtaksbrevet" }
    }

    private fun validerSammeSignatur(vedtaksbrev: Vedtaksbrev) {
        brukerfeilHvis(vedtaksbrev.saksbehandlersignatur != SikkerhetContext.hentSaksbehandlerNavn(
                strict = true)) {
            "En annen saksbehandler har signert vedtaksbrevet"
        }
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }

}
