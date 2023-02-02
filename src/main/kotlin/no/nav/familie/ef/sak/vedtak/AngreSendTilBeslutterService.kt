package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AngreSendTilBeslutterService(
    private val vedtakService: VedtakService,
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
    private val stegService: StegService
) {

    @Transactional
    fun angreSendTilBeslutter(behandlingId: UUID) {
        val vedtak = vedtakService.hentVedtak(behandlingId = behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        validerKanAngreSendTilBeslutter(saksbehandling, vedtak)

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = behandlingId,
            stegtype = saksbehandling.steg,
            utfall = StegUtfall.ANGRE_SEND_TIL_BESLUTTER,
            metadata = null
        )

        ferdigstillGodkjenneVedtakOppgave(saksbehandling)
        opprettBehandleSakOppgave(saksbehandling)

        stegService.angreSendTilBeslutter(behandlingId)
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

    private fun validerKanAngreSendTilBeslutter(saksbehandling: Saksbehandling, vedtak: Vedtak) {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        feilHvis(vedtak.saksbehandlerIdent != innloggetSaksbehandler, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter om du ikke er saksbehandler på vedtaket" }

        val efOppgave = oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.GodkjenneVedtak, saksbehandling = saksbehandling) ?: error("Fant ingen godkjenne vedtak oppgave")
        val tilordnetRessurs = oppgaveService.hentOppgave(efOppgave.gsakOppgaveId).tilordnetRessurs
        val oppgaveErTilordnetEnAnnenSaksbehandler = tilordnetRessurs != null && tilordnetRessurs != innloggetSaksbehandler
        feilHvis(oppgaveErTilordnetEnAnnenSaksbehandler, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter når oppgave er plukket av $tilordnetRessurs" }
    }
}
