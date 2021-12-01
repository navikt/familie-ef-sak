package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.felles.util.DatoFormat.DATE_FORMAT_ISO_YEAR_MONTH
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.YearMonth

/**
 * Slettes etter att den er kjørt
 */
@Profile("!integrasjonstest")
@Configuration
class OpprettFørsteUttrekkArbeidsssøkerTask(private val opprettUttrekkArbeidssøkerTask: OpprettUttrekkArbeidssøkerTask) {

    private var opprettetFørsteTasken = false
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelay = 0, fixedDelay = 30 * 24 * 60 * 1000)
    fun opprettFørsteTasken() {
        if (opprettetFørsteTasken) return
        opprettetFørsteTasken = true
        try {
            opprettUttrekkArbeidssøkerTask.opprettTaskForNesteMåned()
            logger.info("Opprettet task for å opprette uttrekk for arbeidssøker")
        } catch (e: Exception) {
            logger.warn("Feilet opprettelse av task for å opprette uttrekk for arbeidssøker - ${e.message}")
        }
    }
}

@Service
@TaskStepBeskrivelse(
        taskStepType = OpprettUttrekkArbeidssøkerTask.TYPE,
        beskrivelse = "Oppretter uttrekk av arbeidssøkere"
)
class OpprettUttrekkArbeidssøkerTask(
        private val uttrekkArbeidssøkerService: UttrekkArbeidssøkerService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        uttrekkArbeidssøkerService.opprettUttrekkArbeidssøkere()
    }

    override fun onCompletion(task: Task) {
        opprettTaskForNesteMåned()
    }

    fun opprettTaskForNesteMåned() {
        val nesteMåned = YearMonth.now().plusMonths(1)
        val triggerTid = nesteMåned.atDay(1).atTime(5, 0)
        taskRepository.save(Task(TYPE, nesteMåned.format(DATE_FORMAT_ISO_YEAR_MONTH)).medTriggerTid(triggerTid))
    }

    companion object {

        const val TYPE = "opprettUttrekkArbeidssøker"
    }
}
