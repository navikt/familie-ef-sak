package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class SendTilBeslutterSteg(private val taskRepository: TaskRepository,
                           private val oppgaveService: OppgaveService,
                           private val fagsakService: FagsakService,
                           private val behandlingService: BehandlingService,
                           private val vedtaksbrevRepository: VedtaksbrevRepository,
                           private val vedtakService: VedtakService) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }

        if (behandling.type !== BehandlingType.BLANKETT && !vedtaksbrevRepository.existsById(behandling.id)) {
            throw Feil("Brev mangler for behandling=${behandling.id}")
        }
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FATTER_VEDTAK)
        vedtakService.oppdaterSaksbehandler(behandling.id, SikkerhetContext.hentSaksbehandler(strict = true))
        opprettGodkjennVedtakOppgave(behandling)

        ferdigstillOppgave(behandling, Oppgavetype.BehandleSak)
        ferdigstillOppgave(behandling, Oppgavetype.BehandleUnderkjentVedtak)
    }

    private fun ferdigstillOppgave(behandling: Behandling, oppgavetype: Oppgavetype) {
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id, oppgavetype, it.gsakOppgaveId, aktivIdent))
        }
    }

    private fun opprettGodkjennVedtakOppgave(behandling: Behandling) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = behandling.id,
                                       oppgavetype = Oppgavetype.GodkjenneVedtak)))
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }

}