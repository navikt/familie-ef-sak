package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillOppgaveTask.TYPE,
    beskrivelse = "Avslutt oppgave i GOSYS",
    maxAntallFeil = 3,
)
class FerdigstillOppgaveTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    /**
     * Då payload er unik per task type, så settes unik inn
     */
    data class FerdigstillOppgaveTaskData(
        val behandlingId: UUID,
        val oppgavetype: Oppgavetype,
        val unik: LocalDateTime? = LocalDateTime.now(),
    )

    override fun doTask(task: Task) {
        val data = jsonMapper.readValue<FerdigstillOppgaveTaskData>(task.payload)
        oppgaveService.ferdigstillBehandleOppgave(
            behandlingId = data.behandlingId,
            oppgavetype = data.oppgavetype,
        )
    }

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            oppgavetype: Oppgavetype,
            oppgaveId: Long?,
            personIdent: String?,
        ): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(FerdigstillOppgaveTaskData(behandlingId, oppgavetype)),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
                        this["behandlingId"] = behandlingId.toString()
                        this["oppgavetype"] = oppgavetype.name
                        this["oppgaveId"] = oppgaveId.toString()
                        this["personIdent"] = personIdent ?: "ukjent"
                    },
            )

        const val TYPE = "ferdigstillOppgave"
    }
}
