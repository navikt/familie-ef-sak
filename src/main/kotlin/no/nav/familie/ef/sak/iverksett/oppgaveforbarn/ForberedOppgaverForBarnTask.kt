package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.kontrakter.felles.objectMapper
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
        taskStepType = ForberedOppgaverForBarnTask.TYPE,
        beskrivelse = "Oppretter oppgave for barn som fyller 1/2 eller 1 år",

        )
class ForberedOppgaverForBarnTask(val taskRepository: TaskRepository,
                                  val forberedOppgaverForBarnService: ForberedOppgaveForBarnService,
                                  val featureToggleService: FeatureToggleService) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    val DATE_FORMAT_ISO_YEAR_MONTH_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val INNEN_ANTALL_UKER = 1L

    override fun doTask(task: Task) {
        if (featureToggleService.isEnabled("familie.ef.iverksett.opprett-oppgaver-barnsomfylleraar")) {
            val sisteKjøring = objectMapper.readValue<LocalDate>(task.payload).minusWeeks(1)
            forberedOppgaverForBarnService.forberedOppgaverForAlleBarnSomFyllerAar(INNEN_ANTALL_UKER, sisteKjøring)
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
        taskRepository.save(Task(TYPE, nesteUke.format(DATE_FORMAT_ISO_YEAR_MONTH_DAY)).medTriggerTid(triggerTid))
    }

    companion object {

        const val TYPE = "forberedOppgaverForBarnTask"
    }
}