package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettTasksForBarnFyltÅrTask.TYPE,
    beskrivelse = "Oppretter en task pr barn som har fylt 1/2 eller 1 år"
)
class OpprettTasksForBarnFyltÅrTask(
    val taskRepository: TaskRepository,
    val barnFyllerÅrOppfølgingsoppgaveService: BarnFyllerÅrOppfølgingsoppgaveService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr()
    }

    override fun onCompletion(task: Task) {
        taskRepository.save(opprettTask(LocalDate.now().plusDays(1)))
    }

    companion object {

        const val TYPE = "opprettOppfølgingsoppgaverForBarnFyltÅrTask"

        fun opprettTask(dato: LocalDate): Task {
            return Task(TYPE, dato.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .medTriggerTid(dato.atTime(22, 0))
                .apply {
                    this.metadata[MDCConstants.MDC_CALL_ID] = IdUtils.generateId()
                }
        }
    }
}
