package no.nav.familie.ef.sak.behandling.oppgavekontroll

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingUtenOppgaveTask.TYPE,
    maxAntallFeil = 2,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L * 60L * 2,
    beskrivelse = "Finn åpne behandlinger uten behandle sak oppgave",
)
class BehandlingUtenOppgaveTask(val behandlingsoppgaveService: BehandlingsoppgaveService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        behandlingsoppgaveService.validerHarIkkeÅpneBehandligerUtenOppgave()
    }

    companion object {

        const val TYPE = "finnBehandlingUtenOppgave"

        fun opprettTask(): Task {
            return Task(
                TYPE,
                "finnBehandlingUtenOppgave",
                Properties(),
            )
        }
    }
}
