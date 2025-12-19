package no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = StartUtsendingAvAktivitetspliktBrevTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = false,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Start prosess for innhenting av aktivitetsplikt for de i utdanning",
)
class StartUtsendingAvAktivitetspliktBrevTask(
    private val automatiskBrevInnhentingAktivitetspliktService: AutomatiskBrevInnhentingAktivitetspliktService,
) : AsyncTaskStep {
    val logger = Logg.getLogger(this::class)

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<PayLoad>(task.payload)
        automatiskBrevInnhentingAktivitetspliktService.opprettTasks(
            liveRun = payload.aktivitetspliktRequest.liveRun,
            taskLimit = payload.aktivitetspliktRequest.taskLimit,
        )
    }

    companion object {
        fun opprettTask(aktivitetspliktRequest: AktivitetspliktRequest): Task {
            val payload = objectMapper.writeValueAsString(PayLoad(aktivitetspliktRequest, LocalDateTime.now()))

            val properties =
                Properties().apply {
                    setProperty("limit", aktivitetspliktRequest.taskLimit.toString())
                    setProperty("liveRun", aktivitetspliktRequest.liveRun.toString())
                    setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
                }

            return Task(TYPE, payload).copy(metadataWrapper = PropertiesWrapper(properties))
        }

        const val TYPE = "StartUtsendingAvAktivitetspliktBrevTask"
    }
}

private data class PayLoad(
    val aktivitetspliktRequest: AktivitetspliktRequest,
    val tidspunkt: LocalDateTime,
)
