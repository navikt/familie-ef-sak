package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TotrinnskontrollService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.task.IverksettMotOppdragTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeslutteVedtakSteg(private val taskRepository: TaskRepository,
                         private val fagsakService: FagsakService,
                         private val oppgaveService: OppgaveService,
                         private val totrinnskontrollService: TotrinnskontrollService) : BehandlingSteg<BeslutteVedtakDto> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }
    }

    override fun utførOgReturnerNesteSteg(behandling: Behandling, data: BeslutteVedtakDto): StegType {
        totrinnskontrollService.lagreTotrinnskontroll(behandling, data)

        ferdigstillOppgave(behandling)

        return if (data.godkjent) {
            // TODO oppdater brev
            opprettTaskForIverksettMotOppdrag(behandling)
            stegType().hentNesteSteg(behandling.type)
        } else {
            opprettBehandleUnderkjentVedtakOppgave(behandling)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun ferdigstillOppgave(behandling: Behandling) {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id, oppgavetype))
        }
    }

    private fun opprettBehandleUnderkjentVedtakOppgave(behandling: Behandling) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = behandling.id,
                                       oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                                       fristForFerdigstillelse = LocalDate.now(),
                                       tilordnetNavIdent = SikkerhetContext.hentSaksbehandler())))
    }

    private fun opprettTaskForIverksettMotOppdrag(behandling: Behandling) {
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        taskRepository.save(IverksettMotOppdragTask.opprettTask(behandling, fagsak.hentAktivIdent()))
    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    override fun utførSteg(behandling: Behandling, data: BeslutteVedtakDto) {
        error("Bruker utførOgReturnerNesteSteg")
    }

}