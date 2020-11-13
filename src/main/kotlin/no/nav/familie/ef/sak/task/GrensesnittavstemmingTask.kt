package no.nav.familie.ef.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.objectMapper
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
        val payload = objectMapper.readValue<GrensesnittavstemmingPayload>(task.payload)

        val stønadstype = payload.stønadstype
        val fraTidspunkt = payload.fraDato.atStartOfDay()
        val tilTidspunkt = task.triggerTid.toLocalDate().atStartOfDay()

        logger.info("Gjør ${task.id} $stønadstype avstemming mot oppdrag fra $fraTidspunkt til $tilTidspunkt")
        avstemmingService.grensesnittavstemOppdrag(fraTidspunkt, tilTidspunkt, stønadstype)

    }

    override fun onCompletion(task: Task) {
        val payload = objectMapper.readValue<GrensesnittavstemmingPayload>(task.payload)
        val nesteVirkedag: LocalDate = VirkedagerProvider.nesteVirkedag(task.triggerTid.toLocalDate())

        opprettNyTask(task.triggerTid.toLocalDate(), nesteVirkedag.atTime(8, 0), payload.stønadstype)
    }

    fun utløsGrensesnittavstemming(fraDato: LocalDate, stønadstype: Stønadstype, triggerTid: LocalDateTime?): Task {
        val nesteVirkedag: LocalDateTime = triggerTid ?: VirkedagerProvider.nesteVirkedag(fraDato).atTime(8, 0)

        return opprettNyTask(fraDato, nesteVirkedag, stønadstype)
    }

    private fun opprettNyTask(fraDato: LocalDate,
                              nesteVirkedag: LocalDateTime,
                              stønadstype: Stønadstype): Task {
        val grensesnittavstemmingPayload = GrensesnittavstemmingPayload(fraDato = fraDato, stønadstype = stønadstype)

        val nesteAvstemmingTask = Task(type = TYPE,
                                       payload = objectMapper.writeValueAsString(grensesnittavstemmingPayload),
                                       triggerTid = nesteVirkedag)

        return taskRepository.save(nesteAvstemmingTask)
    }

    companion object {

        const val TYPE = "utførGrensesnittavstemming"
    }

}