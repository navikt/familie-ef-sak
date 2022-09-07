package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppfølgingsoppgaveForBarnFyltÅrTask.TYPE,
    beskrivelse = "Oppretter oppfølgingsoppgave for barn som har fylt 1/2 eller 1 år"
)
class OpprettOppfølgingsoppgaveForBarnFyltÅrTask(
    val taskRepository: TaskRepository,
    val oppgaveClient: OppgaveClient,
    val oppgaveRepository: OppgaveRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(task.payload)
        val opprettetOppgaveId = oppgaveClient.opprettOppgave(opprettOppgavePayload.opprettOppgaveRequest)
        val oppgave = Oppgave(
            gsakOppgaveId = opprettetOppgaveId,
            behandlingId = opprettOppgavePayload.behandlingId,
            barnPersonIdent = opprettOppgavePayload.barnPersonIdent,
            type = Oppgavetype.InnhentDokumentasjon,
            alder = opprettOppgavePayload.alder
        )
        oppgaveRepository.insert(oppgave)
    }

    companion object {

        const val TYPE = "opprettOppfølgingsoppgaveForBarnFyltÅrTask"

        fun opprettTask(opprettOppgavePayload: OpprettOppgavePayload): Task {
            return Task(TYPE, objectMapper.writeValueAsString(opprettOppgavePayload))
        }
    }
}

data class OpprettOppgavePayload(
    val behandlingId: UUID,
    val barnPersonIdent: String,
    val alder: Alder,
    val opprettOppgaveRequest: OpprettOppgaveRequest
)
