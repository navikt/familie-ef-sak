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
    taskStepType = ForberedOppgaverForBarnTask.TYPE,
    beskrivelse = "Oppretter oppgave for barn som fyller 1/2 eller 1 år"
)
class ForberedOppgaverForBarnTask(
    val taskRepository: TaskRepository,
    val forberedOppgaverForBarnService: ForberedOppgaverForBarnService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val referansedato = LocalDate.parse(task.payload)
        forberedOppgaverForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(referansedato)
    }

    override fun onCompletion(task: Task) {
        val sisteKjøring = LocalDate.parse(task.payload)
        opprettTaskForNesteUke(sisteKjøring)
    }

    fun opprettTaskForNesteUke(sisteKjøring: LocalDate) {
        val nesteUke = sisteKjøring.plusWeeks(2)
        taskRepository.save(opprettTask(nesteUke))
    }

    companion object {

        const val TYPE = "forberedOppgaverForBarnTask"

        fun opprettTask(dato: LocalDate): Task {
            return Task(OpprettTasksForBarnFyltÅrTask.TYPE, dato.format(DateTimeFormatter.ISO_LOCAL_DATE)).medTriggerTid(dato.atTime(5, 0))
        }
    }
}
