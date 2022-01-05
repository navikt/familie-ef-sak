package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.felles.util.DatoFormat.DATE_FORMAT_ISO_YEAR_MONTH
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
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
