package no.nav.familie.ef.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = OpprettOppgaveTask.TYPE,
                     beskrivelse = "Opprett oppgave i GOSYS for behandling",
                     maxAntallFeil = 3)
class OpprettOppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    data class OpprettOppgaveTaskData(val behandlingId: UUID,
                                      val oppgavetype: Oppgavetype,
                                      val fristForFerdigstillelse: LocalDate,
                                      val tilordnetNavIdent: String? = null,
                                      val beskrivelse: String? = null)

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(task.payload)
        val oppgaveId = oppgaveService.opprettOppgave(behandlingId = data.behandlingId,
                                                      oppgavetype = data.oppgavetype,
                                                      fristForFerdigstillelse = data.fristForFerdigstillelse,
                                                      tilordnetNavIdent = data.tilordnetNavIdent,
                                                      beskrivelse = data.beskrivelse)
        task.metadata.setProperty("oppgaveId", oppgaveId.toString())
    }

    companion object {

        fun opprettTask(data: OpprettOppgaveTaskData): Task {
            return Task(type = TYPE,
                        payload = objectMapper.writeValueAsString(data),
                        properties = Properties().apply {
                            this["saksbehandler"] = SikkerhetContext.hentSaksbehandler()
                        })

        }

        const val TYPE = "opprettOppgave"
    }


}