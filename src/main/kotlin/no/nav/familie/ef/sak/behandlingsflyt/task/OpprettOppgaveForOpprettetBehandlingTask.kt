package no.nav.familie.ef.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTask.TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for opprettet revurdering",
    maxAntallFeil = 3
)
class OpprettOppgaveForOpprettetBehandlingTask(
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService
) : AsyncTaskStep {

    data class OpprettOppgaveTaskData(
        val behandlingId: UUID,
        val saksbehandler: String,
        val beskrivelse: String? = null,
        val hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
        val mappeId: Long? = null
    )

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(task.payload)
        val oppgaveId = opprettOppgave(data)
        task.metadata.setProperty("oppgaveId", oppgaveId.toString())

        taskService.save(
            BehandlingsstatistikkTask.opprettMottattTask(
                behandlingId = data.behandlingId,
                hendelseTidspunkt = data.hendelseTidspunkt,
                oppgaveId = oppgaveId,
                saksbehandler = data.saksbehandler
            )
        )
    }

    private fun opprettOppgave(data: OpprettOppgaveTaskData): Long {
        val tilordnetNavIdent =
            if (data.saksbehandler == SikkerhetContext.SYSTEM_FORKORTELSE) null else data.saksbehandler
        return oppgaveService.opprettOppgave(
            behandlingId = data.behandlingId,
            oppgavetype = Oppgavetype.BehandleSak,
            tilordnetNavIdent = tilordnetNavIdent,
            beskrivelse = data.beskrivelse,
            mappeId = data.mappeId
        )
    }

    companion object {

        fun opprettTask(data: OpprettOppgaveTaskData): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(data),
                properties = Properties().apply {
                    this["saksbehandler"] = data.saksbehandler
                    this["behandlingId"] = data.behandlingId.toString()
                }
            )
        }

        const val TYPE = "opprettOppgaveForOpprettetBehandling"
    }
}
