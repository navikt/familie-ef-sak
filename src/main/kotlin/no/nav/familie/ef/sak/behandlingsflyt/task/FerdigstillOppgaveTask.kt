package no.nav.familie.ef.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = FerdigstillOppgaveTask.TYPE,
                     beskrivelse = "Avslutt oppgave i GOSYS",
                     maxAntallFeil = 3)
class FerdigstillOppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    /**
     * Då payload er unik per task type, så settes unik inn
     */
    data class FerdigstillOppgaveTaskData(val behandlingId: UUID,
                                          val oppgavetype: Oppgavetype,
                                          val unik: LocalDateTime? = LocalDateTime.now())

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<FerdigstillOppgaveTaskData>(task.payload)
        oppgaveService.ferdigstillBehandleOppgave(behandlingId = data.behandlingId,
                                                  oppgavetype = data.oppgavetype)
    }

    companion object {

        fun opprettTask(behandlingId: UUID, oppgavetype: Oppgavetype, oppgaveId: Long, personIdent: String): Task {
            return Task(type = TYPE,
                        payload = objectMapper.writeValueAsString(FerdigstillOppgaveTaskData(behandlingId, oppgavetype)),
                        properties = Properties().apply {
                            this["saksbehandler"] = SikkerhetContext.hentSaksbehandler()
                            this["behandlingId"] = behandlingId.toString()
                            this["oppgavetype"] = oppgavetype.name
                            this["oppgaveId"] = oppgaveId.toString()
                            this["personIdent"] = personIdent
                        })

        }

        const val TYPE = "ferdigstillOppgave"
    }


}