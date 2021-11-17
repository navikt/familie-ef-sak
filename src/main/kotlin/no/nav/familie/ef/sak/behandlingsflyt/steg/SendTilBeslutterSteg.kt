package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
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
                           private val tilbakekrevingService: TilbakekrevingService) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }

        if (behandling.type !== BehandlingType.BLANKETT && !vedtaksbrevRepository.existsById(behandling.id)) {
            throw Feil("Brev mangler for behandling=${behandling.id}")
        }
        feilHvis(saksbehandlerMåTaStilingTilTilbakekreving(behandling)) {
            "Feilutbetaling detektert. Må ta stilling til feilutbetalingsvarsel under simulering"
        }
    }

    private fun saksbehandlerMåTaStilingTilTilbakekreving(behandling: Behandling): Boolean {
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

    private fun erIkkeRelevantForTilbakekreving(behandling: Behandling): Boolean {
        val resultatType = vedtakService.hentVedtak(behandling.id).resultatType
        return behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING || behandling.type == BehandlingType.BLANKETT || resultatType == ResultatType.AVSLÅ || resultatType == ResultatType.HENLEGGE
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FATTER_VEDTAK)
        vedtakService.oppdaterSaksbehandler(behandling.id, SikkerhetContext.hentSaksbehandler(strict = true))
        opprettGodkjennVedtakOppgave(behandling)
        ferdigstillOppgave(behandling, Oppgavetype.BehandleSak)
        ferdigstillOppgave(behandling, Oppgavetype.BehandleUnderkjentVedtak)
        opprettTaskForBehandlingsstatistikk(behandling.id)
    }

    private fun opprettTaskForBehandlingsstatistikk(behandlingId: UUID) =
            taskRepository.save(BehandlingsstatistikkTask.opprettVedtattTask(behandlingId = behandlingId))


    private fun ferdigstillOppgave(behandling: Behandling, oppgavetype: Oppgavetype) {
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id,
                                                                   oppgavetype,
                                                                   it.gsakOppgaveId,
                                                                   aktivIdent))
        }
    }

    private fun opprettGodkjennVedtakOppgave(behandling: Behandling) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = behandling.id,
                                       oppgavetype = Oppgavetype.GodkjenneVedtak,
                                       beskrivelse = "Sendt til godkjenning av ${SikkerhetContext.hentSaksbehandlerNavn(true)}.")))
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }

}
