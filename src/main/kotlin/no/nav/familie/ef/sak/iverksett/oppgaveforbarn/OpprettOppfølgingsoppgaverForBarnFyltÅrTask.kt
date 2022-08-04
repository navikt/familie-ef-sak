package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppfølgingsoppgaverForBarnFyltÅrTask.TYPE,
    beskrivelse = "Oppretter oppgave for barn som har fylt 1/2 eller 1 år"
)
class OpprettOppfølgingsoppgaverForBarnFyltÅrTask(
    val taskRepository: TaskRepository,
    val barnFyllerÅrOppfølgingsoppgaveService: BarnFyllerÅrOppfølgingsoppgaveService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        barnFyllerÅrOppfølgingsoppgaveService.opprettOppgaverForAlleBarnSomHarFyltÅr()
    }

    override fun onCompletion(task: Task) {
        taskRepository.save(opprettTask(LocalDate.now().plusDays(1)))
    }

    companion object {

        const val TYPE = "opprettOppfølgingsoppgaverForBarnFyltÅrTask"

        fun opprettTask(dato: LocalDate): Task {
            return Task(TYPE, dato.format(DateTimeFormatter.ISO_LOCAL_DATE)).medTriggerTid(dato.atTime(22, 0))
        }
    }
}
