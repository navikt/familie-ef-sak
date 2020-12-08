package no.nav.familie.ef.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.avstemming.GrensesnittavstemmingDto
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate


data class GrensesnittavstemmingPayload(val fraDato: LocalDate, val stønadstype: Stønadstype)

@Service
@TaskStepBeskrivelse(taskStepType = GrensesnittavstemmingTask.TYPE, beskrivelse = "Utfører grensesnittavstemming mot økonomi.")
class GrensesnittavstemmingTask(private val avstemmingService: AvstemmingService) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun doTask(task: Task) {
        with(objectMapper.readValue<GrensesnittavstemmingPayload>(task.payload)) {
            val fraTidspunkt = fraDato.atStartOfDay()
            val tilTidspunkt = task.triggerTid.toLocalDate().atStartOfDay()

            logger.info("Gjør ${task.id} $stønadstype avstemming mot oppdrag fra $fraTidspunkt til $tilTidspunkt")
            avstemmingService.grensesnittavstemOppdrag(fraTidspunkt, tilTidspunkt, stønadstype)
        }
    }

    override fun onCompletion(task: Task) {
        val payload = objectMapper.readValue<GrensesnittavstemmingPayload>(task.payload)
        val nesteFradato = task.triggerTid.toLocalDate()
        avstemmingService.opprettGrensesnittavstemmingTask(GrensesnittavstemmingDto(stønadstype = payload.stønadstype,
                                                                                    fraDato = nesteFradato))
    }

    companion object {

        const val TYPE = "utførGrensesnittavstemming"
    }

}