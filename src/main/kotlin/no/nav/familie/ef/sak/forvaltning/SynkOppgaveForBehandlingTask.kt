package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SynkOppgaveForBehandlingTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Synkroniser oppgave for behandling",
)
class SynkOppgaveForBehandlingTask(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)

        val oppgavetype =
            if (behandling.status == BehandlingStatus.UTREDES) {
                Oppgavetype.BehandleSak
            } else if (behandling.status == BehandlingStatus.FATTER_VEDTAK) {
                Oppgavetype.GodkjenneVedtak
            } else {
                throw NotImplementedError("Støtter ikke synkronisering av behandlingstatus ${behandling.status}")
            }

        oppgaveService.ferdigstillOppgaverForBehandlingId(behandlingId)
        oppgaveService.opprettOppgave(behandlingId, oppgavetype)
    }

    companion object {
        const val TYPE = "synkOppgaveForBehandlingTask"

        fun opprettTask(behandlingId: UUID): Task =
            Task(
                TYPE,
                behandlingId.toString(),
                Properties(),
            )
    }
}
