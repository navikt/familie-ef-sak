package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.VedtaksbrevService
import no.nav.familie.ef.sak.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SendTilBeslutterSteg(private val taskRepository: TaskRepository,
                           private val oppgaveService: OppgaveService,
                           private val behandlingService: BehandlingService,
                           private val vedtaksbrevService: VedtaksbrevService) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FATTER_VEDTAK)
        opprettGodkjennVedtakOppgave(behandling)

        ferdigstillOppgave(behandling, Oppgavetype.BehandleSak)
        ferdigstillOppgave(behandling, Oppgavetype.BehandleUnderkjentVedtak)
        if(behandling.type !== BehandlingType.BLANKETT) {
            vedtaksbrevService.lagreBrevUtkast(behandling.id)
        }
    }

    private fun ferdigstillOppgave(behandling: Behandling, oppgavetype: Oppgavetype) {
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id, oppgavetype))
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