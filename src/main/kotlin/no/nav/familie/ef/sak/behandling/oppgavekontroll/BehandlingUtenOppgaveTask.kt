package no.nav.familie.ef.sak.behandling.oppgavekontroll

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingUtenOppgaveTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Finn åpne behandlinger uten behandle sak oppgave",
)
class BehandlingUtenOppgaveTask(
    val behandlingsoppgaveService: BehandlingsoppgaveService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val antallÅpneBehandlingerUtenOppgave = behandlingsoppgaveService.antallÅpneBehandlingerUtenOppgave()
        feilHvis(antallÅpneBehandlingerUtenOppgave > 0) { "Åpne behandlinger uten behandleSak oppgave funnet på fagsak " }
    }

    companion object {
        const val TYPE = "finnBehandlingUtenOppgave"

        fun opprettTask(payloadÅrOgUke: String): Task =
            Task(
                TYPE,
                payloadÅrOgUke,
                Properties(),
            )
    }
}
