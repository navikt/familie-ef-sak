package no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = NæringsinntektKontrollForOppgaveTask.TYPE,
    beskrivelse = "Kontroller næringsinntekt for oppgave med frist 15. desember i mappe for selvstendig næringsdrivende",
)
class NæringsinntektKontrollForOppgaveTask(
    private val næringsinntektKontrollService: NæringsinntektKontrollService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(LocalDate.now().year - 1, task.payload.toLong())
    }

    companion object {
        const val TYPE = "næringsinntektKontrollForOppgaveTask"

        fun opprettTask(oppgaveId: Long): Task =
            Task(
                TYPE,
                oppgaveId.toString(),
                Properties().apply {
                    this["oppgaveId"] = oppgaveId.toString()
                },
            )
    }
}
