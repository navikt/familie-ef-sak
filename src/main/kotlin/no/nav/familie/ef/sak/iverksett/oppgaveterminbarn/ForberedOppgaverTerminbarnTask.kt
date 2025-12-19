package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@TaskStepBeskrivelse(
    taskStepType = ForberedOppgaverTerminbarnTask.TYPE,
    beskrivelse = "Oppretter oppgave for ufødte terminbarn",
)
class ForberedOppgaverTerminbarnTask(
    val taskService: TaskService,
    val forberedOppgaverTerminbarnService: ForberedOppgaverTerminbarnService,
    val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {
    private val logger = Logg.getLogger(this::class)

    override fun doTask(task: Task) {
        logger.info("Starter forbereding av oppgaver for ufødte terminbarn")
        forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn()
    }

    override fun onCompletion(task: Task) {
        opprettTaskForNesteUke()
    }

    fun opprettTaskForNesteUke() {
        val nesteUke = LocalDate.now().plusWeeks(1)
        val triggerTid = nesteUke.atTime(5, 0)
        taskService.save(Task(TYPE, LocalDate.now().format(DateTimeFormatter.ISO_DATE)).medTriggerTid(triggerTid))
    }

    companion object {
        const val TYPE = "forberedOppgaverForTerminbarnTask"
    }
}
