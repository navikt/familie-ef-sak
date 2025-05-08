package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.oppfølgingsoppgave.OppfølgingsoppgaveService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendAutomatiskBrevTask.TYPE,
    beskrivelse = "Sender automatisk brev",
)
class SendAutomatiskBrevTask(
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        oppfølgingsoppgaveService.sendAutomatiskBrev(behandlingId)
    }

    companion object {
        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingId.toString()
                    },
            )

        const val TYPE = "sendAutomatiskBrevTask"
    }
}
