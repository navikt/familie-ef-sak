package no.nav.familie.ef.sak.behandling.oppgavekontroll

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingUtenOppgaveTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Finn åpne behandlinger uten behandle sak oppgave",
)
class BehandlingUtenOppgaveTask(val behandlingsoppgaveService: BehandlingsoppgaveService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        behandlingsoppgaveService.loggÅpneBehandlingerUtenOppgave()
    }

    companion object {

        const val TYPE = "finnBehandlingUtenOppgave"

        fun opprettTask(ukenummer: Int): Task {
            return Task(
                TYPE,
                ukenummer.toString(),
                Properties(),
            )
        }
    }
}
