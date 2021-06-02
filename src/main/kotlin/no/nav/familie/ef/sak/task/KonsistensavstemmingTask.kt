package no.nav.familie.ef.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class KonsistensavstemmingPayload(val stønadstype: Stønadstype, val triggerTid: LocalDateTime)

@Service
@TaskStepBeskrivelse(taskStepType = KonsistensavstemmingTask.TYPE, beskrivelse = "Utfører konsistensavstemming mot økonomi.")
class KonsistensavstemmingTask(
        private val avstemmingService: AvstemmingService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<KonsistensavstemmingPayload>(task.payload)
        avstemmingService.konsistensavstemOppdrag(payload.stønadstype, payload.triggerTid.toLocalDate())
    }

    companion object {

        const val TYPE = "utførKonsistensavstemming"
    }


}