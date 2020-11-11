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

@Service
@TaskStepBeskrivelse(taskStepType = GrensesnittavstemmingTask.TYPE, beskrivelse = "Utfører grensesnittavstemming mot økonomi.")
class GrensesnittavstemmingTask(private val avstemmingService: AvstemmingService,
                                private val taskRepository: TaskRepository) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<GrensesnittavstemmingPayload>(task.payload)

        val fraTidspunkt = payload.fraDato.atStartOfDay()
        val tilTidspunkt = task.triggerTid.toLocalDate().atStartOfDay()

        logger.info("Gjør ${task.id} ${payload.stønadstype} avstemming mot oppdrag fra $fraTidspunkt til $tilTidspunkt")

        when (payload.stønadstype) {
            Stønadstype.OVERGANGSSTØNAD -> avstemmingService.grensesnittavstemOvergangsstønad(fraTidspunkt, tilTidspunkt)
            else -> throw Error("Grensesnittavstemming for ${payload.stønadstype} er ikke implementert")
        }
    }

    override fun onCompletion(task: Task) {
        val nesteVirkedag: LocalDate = VirkedagerProvider.nesteVirkedag(task.triggerTid.toLocalDate())

        opprettNyTask(task.triggerTid.toLocalDate(), nesteVirkedag)
    }

    fun utløsGrensesnittavstemming(fraDato: LocalDate, stønadstype: Stønadstype, triggerTid: LocalDate?): Task {
        val nesteVirkedag: LocalDate = triggerTid ?: VirkedagerProvider.nesteVirkedag(fraDato)

        return opprettNyTask(fraDato, nesteVirkedag, stønadstype)
    }

    private fun opprettNyTask(fraDato: LocalDate,
                              nesteVirkedag: LocalDate,
                              stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD): Task {
        val grensesnittavstemmingPayload = GrensesnittavstemmingPayload(fraDato = fraDato, stønadstype = stønadstype)

        val nesteAvstemmingTask = Task(type = TYPE,
                                       payload = objectMapper.writeValueAsString(grensesnittavstemmingPayload),
                                       triggerTid = nesteVirkedag.atTime(8, 0))

        return taskRepository.save(nesteAvstemmingTask)
    }

    companion object {

        const val TYPE = "utførGrensesnittavstemming"
    }

}