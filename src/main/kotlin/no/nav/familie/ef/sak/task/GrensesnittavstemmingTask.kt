package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.util.VirkedagerProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(taskStepType = GrensesnittavstemmingTask.TYPE, beskrivelse = "Utfører grensesnittavstemming mot økonomi.")
class GrensesnittavstemmingTask(private val avstemmingService: AvstemmingService,
                                private val taskRepository: TaskRepository) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun doTask(task: Task) {
        val fraTidspunkt = LocalDate.parse(task.payload).atStartOfDay()
        val tilTidspunkt = task.triggerTid.toLocalDate().atStartOfDay()

        logger.info("Gjør avstemming mot oppdrag fra $fraTidspunkt til ${task.triggerTid.toLocalDate()}")

        avstemmingService.grensesnittavstemOppdrag(fraTidspunkt, tilTidspunkt)

    }

    override fun onCompletion(task: Task) {
        val nesteVirkedag: LocalDate = VirkedagerProvider.nesteVirkedag(task.triggerTid.toLocalDate())

        val nesteAvstemmingTask = Task(type = TYPE,
                                       payload = task.triggerTid.toLocalDate().toString(),
                                       triggerTid = nesteVirkedag.atTime(8, 0))

        taskRepository.save(nesteAvstemmingTask)
    }

    companion object {

        const val TYPE = "utførGrensesnittavstemming"
    }

}