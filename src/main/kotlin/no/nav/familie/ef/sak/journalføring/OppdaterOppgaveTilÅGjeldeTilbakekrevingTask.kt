package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.klage.KlageClient
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterOppgaveTilÅGjeldeTilbakekrevingTask.TYPE,
    beskrivelse = "Oppdater behandlingstema for behandle sak oppgave i klage til tilbakekreving",
    maxAntallFeil = 3,
)
class OppdaterOppgaveTilÅGjeldeTilbakekrevingTask(private val klageClient: KlageClient) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        klageClient.oppdaterOppgaveTilÅGjeldeTilbakekreving(behandlingId)
    }

    companion object {

        fun opprettTask(behandlingId: UUID): Task {
            return Task(
                type = TYPE,
                payload = behandlingId.toString()
            )
        }

        const val TYPE = "oppdaterOppgaveTilÅGjeldeTilbakekreving"
    }
}
