package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.felles.util.DatoFormat.DATE_FORMAT_ISO_YEAR_MONTH
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
@TaskStepBeskrivelse(
        taskStepType = OpprettUttrekkArbeidssøkerTask.TYPE,
        beskrivelse = "Oppretter uttrekk av arbeidssøkere"
)
class OpprettUttrekkArbeidssøkerTask(
        private val uttrekkArbeidssøkerService: UttrekkArbeidssøkerService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60 * 1000)
    fun opprettFørsteTasken() {
        try {
            opprettTaskForNesteMåned()
            logger.info("Opprettet task for å opprette uttrekk for arbeidssøker")
        } catch (e: Exception) {
            logger.warn("Feilet opprettelse av task for å opprette uttrekk for arbeidssøker", e)
        }
    }

    override fun doTask(task: Task) {
        uttrekkArbeidssøkerService.opprettUttrekkArbeidssøkere()
    }

    override fun onCompletion(task: Task) {
        opprettTaskForNesteMåned()
    }

    private fun opprettTaskForNesteMåned() {
        val nesteMåned = YearMonth.now().plusMonths(1)
        val triggerTid = nesteMåned.atDay(1).atTime(5, 0)
        taskRepository.save(Task(TYPE, nesteMåned.format(DATE_FORMAT_ISO_YEAR_MONTH)).medTriggerTid(triggerTid))
    }

    companion object {

        const val TYPE = "opprettUttrekkArbeidssøker"
    }
}
