package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegServiceDeprecated
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AngreSendTilBeslutterService(
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService,
    private val stegServiceDeprecated: StegServiceDeprecated,
    private val totrinnskontrollService: TotrinnskontrollService,
) {
    @Transactional
    fun angreSendTilBeslutter(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        validerKanAngreSendTilBeslutter(saksbehandling)

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = behandlingId,
            stegtype = saksbehandling.steg,
            utfall = StegUtfall.ANGRE_SEND_TIL_BESLUTTER,
            metadata = null,
        )

        ferdigstillGodkjenneVedtakOppgave(saksbehandling)
        opprettBehandleSakOppgave(saksbehandling)

        stegServiceDeprecated.angreSendTilBeslutter(behandlingId)
    }

    private fun opprettBehandleSakOppgave(saksbehandling: Saksbehandling) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    beskrivelse = "Angret send til beslutter",
                    tilordnetNavIdent = SikkerhetContext.hentSaksbehandler(),
                ),
            ),
        )
    }

    private fun ferdigstillGodkjenneVedtakOppgave(saksbehandling: Saksbehandling) {
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.GodkjenneVedtak, saksbehandling)?.let {
            taskService.save(
                FerdigstillOppgaveTask.opprettTask(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    it.gsakOppgaveId,
                    personIdent = null,
                ),
            )
        }
    }

    private fun validerKanAngreSendTilBeslutter(saksbehandling: Saksbehandling) {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        val saksbehandlerSendtTilBeslutter = totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(saksbehandling.id)

        brukerfeilHvis(saksbehandlerSendtTilBeslutter != innloggetSaksbehandler) {
            "Kan kun angre send til beslutter dersom du er saksbehandler på vedtaket: $saksbehandlerSendtTilBeslutter vs $innloggetSaksbehandler"
        }

        val efOppgave =
            oppgaveService.hentOppgaveSomIkkeErFerdigstilt(
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                saksbehandling = saksbehandling,
            )
                ?: throw ApiFeil(feil = "Systemet har ikke rukket å opprette godkjenne vedtak oppgaven enda. Prøv igjen om litt.", httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)

        val tilordnetRessurs = oppgaveService.hentOppgave(efOppgave.gsakOppgaveId).tilordnetRessurs
        val oppgaveErTilordnetEnAnnenSaksbehandler =
            tilordnetRessurs != null && tilordnetRessurs != innloggetSaksbehandler
        brukerfeilHvis(oppgaveErTilordnetEnAnnenSaksbehandler) {
            "Kan ikke angre send til beslutter når oppgave er plukket av $tilordnetRessurs"
        }
    }
}
