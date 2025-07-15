package no.nav.familie.ef.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
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
        val sendAutomatiskBrevTaskPayload = objectMapper.readValue<SendAutomatiskBrevTaskPayload>(task.payload)
        oppfølgingsoppgaveService.sendAutomatiskBrev(sendAutomatiskBrevTaskPayload.behandlingId, sendAutomatiskBrevTaskPayload.saksbehandlerIdent)
    }

    companion object {
        fun opprettTask(sendAutomatiskBrevTaskPayload: SendAutomatiskBrevTaskPayload): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(sendAutomatiskBrevTaskPayload),
                properties =
                    Properties().apply {
                        this["behandlingId"] = sendAutomatiskBrevTaskPayload.behandlingId.toString()
                        this["saksbehandlerIdent"] = sendAutomatiskBrevTaskPayload.saksbehandlerIdent
                    },
            )

        const val TYPE = "sendAutomatiskBrevTask"
    }
}

data class SendAutomatiskBrevTaskPayload(
    val behandlingId: UUID,
    val saksbehandlerIdent: String,
)
