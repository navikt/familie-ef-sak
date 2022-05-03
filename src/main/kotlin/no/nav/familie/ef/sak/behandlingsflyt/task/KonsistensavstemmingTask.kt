package no.nav.familie.ef.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.avstemming.AvstemmingService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

data class KonsistensavstemmingPayload(val stønadstype: StønadType,
                                       val triggerTid: LocalDateTime)

@Service
@TaskStepBeskrivelse(taskStepType = KonsistensavstemmingTask.TYPE, beskrivelse = "Utfører konsistensavstemming mot økonomi.")
class KonsistensavstemmingTask(
        private val avstemmingService: AvstemmingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<KonsistensavstemmingPayload>(task.payload)
        avstemmingService.konsistensavstemOppdrag(payload.stønadstype, payload.triggerTid.toLocalDate())
    }

    companion object {

        const val TYPE = "utførKonsistensavstemming"

        fun opprettTask(payload: KonsistensavstemmingPayload): Task {
            return Task(type = TYPE,
                        payload = objectMapper.writeValueAsString(payload),
                        properties = Properties().apply {
                            this["stønadstype"] = payload.stønadstype.name
                        }).copy(triggerTid = payload.triggerTid)
        }
    }


}