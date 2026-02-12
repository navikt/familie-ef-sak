package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.avstemming.AvstemmingService
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

data class KonsistensavstemmingPayload(
    val stønadstype: StønadType,
    val datoForAvstemming: LocalDate,
)

@Service
@TaskStepBeskrivelse(taskStepType = KonsistensavstemmingTask.TYPE, beskrivelse = "Utfører konsistensavstemming mot økonomi.")
class KonsistensavstemmingTask(
    private val avstemmingService: AvstemmingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val payload = jsonMapper.readValue<KonsistensavstemmingPayload>(task.payload)
        avstemmingService.konsistensavstemOppdrag(payload.stønadstype, LocalDateTime.now())
    }

    companion object {
        const val TYPE = "utførKonsistensavstemming"

        fun opprettTask(
            payload: KonsistensavstemmingPayload,
            triggerTid: LocalDateTime,
        ): Task {
            val task =
                Task(
                    type = TYPE,
                    payload = jsonMapper.writeValueAsString(payload),
                    triggerTid = triggerTid,
                )
            val properties =
                PropertiesWrapper(
                    task.metadata.apply {
                        this["stønadstype"] = payload.stønadstype.name
                        this[MDCConstants.MDC_CALL_ID] = IdUtils.generateId()
                    },
                )
            return task.copy(metadataWrapper = properties)
        }
    }
}
