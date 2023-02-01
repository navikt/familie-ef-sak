package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class AngreSendTilBeslutterSteg(
    private val vedtakService: VedtakService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
    private val behandlingService: BehandlingService
) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        ferdigstillGodkjenneVedtakOppgave(saksbehandling)
        opprettBehandleSakOppgave(saksbehandling)
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, status = BehandlingStatus.UTREDES)
    }

    override fun validerSteg(saksbehandling: Saksbehandling) {
        val vedtak = vedtakService.hentVedtak(saksbehandling.id)
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        feilHvis(vedtak.saksbehandlerIdent != innloggetSaksbehandler, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter om du ikke er saksbehandler på vedtaket" }
        feilHvis(saksbehandling.status != BehandlingStatus.FATTER_VEDTAK, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter når behandlingen har status ${saksbehandling.status}" }

        val efOppgave = oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.GodkjenneVedtak, saksbehandling = saksbehandling) ?: error("Fant ingen godkjenne vedtak oppgave")
        val tilordnetRessurs = oppgaveService.hentOppgave(efOppgave.gsakOppgaveId).tilordnetRessurs
        val oppgaveErTilordnetEnAnnenSaksbehandler = tilordnetRessurs != null && tilordnetRessurs != innloggetSaksbehandler
        feilHvis(oppgaveErTilordnetEnAnnenSaksbehandler, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter når oppgave er plukket av $tilordnetRessurs" }
    }

    override fun stegType(): StegType {
        return StegType.ANGRE_SEND_TIL_BESLUTTER
    }

    private fun opprettBehandleSakOppgave(saksbehandling: Saksbehandling) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    beskrivelse = "Angret send til beslutter"
                )
            )
        )
    }

    private fun ferdigstillGodkjenneVedtakOppgave(saksbehandling: Saksbehandling) {
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.GodkjenneVedtak, saksbehandling)?.let {
            taskService.save(
                FerdigstillOppgaveTask.opprettTask(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    it.gsakOppgaveId,
                    personIdent = null
                )
            )
        }
    }
}
