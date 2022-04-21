package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@TaskStepBeskrivelse(
        taskStepType = ForberedOppgaverTerminbarnTask.TYPE,
        beskrivelse = "Oppretter oppgave for ufødte terminbarn")
class ForberedOppgaverTerminbarnTask(val taskRepository: TaskRepository,
                                     val forberedOppgaverTerminbarnService: ForberedOppgaverTerminbarnService,
                                     val featureToggleService: FeatureToggleService) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        if (featureToggleService.isEnabled("familie.ef.iverksett.opprett-oppgaver-terminbarn")) {
            forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn()
        } else {
            logger.warn("Feature toggle opprett-oppgaver-barnsomfylleraar er ikke enablet")
        }
    }

    override fun onCompletion(task: Task) {
        opprettTaskForNesteUke()
    }

    fun opprettTaskForNesteUke() {
        val nesteUke = LocalDate.now().plusWeeks(1)
        val triggerTid = nesteUke.atTime(5, 0)
        taskRepository.save(Task(TYPE, LocalDate.now().format(DateTimeFormatter.ISO_DATE)).medTriggerTid(triggerTid))
    }

    companion object {

        const val TYPE = "forberedOppgaverForBarnTask"
    }
}