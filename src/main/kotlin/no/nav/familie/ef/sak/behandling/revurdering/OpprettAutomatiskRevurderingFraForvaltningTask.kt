package no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import kotlin.collections.set

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettAutomatiskRevurderingFraForvaltningTask.TYPE,
    beskrivelse = "Automatisk revurdering av inntekt - Brukes av endepunktet revurder-personer-med-inntektsendringer-automatisk i forvaltning i personhendelse",
)
class OpprettAutomatiskRevurderingFraForvaltningTask(
    private val automatiskRevurderingService: AutomatiskRevurderingService,
    private val revurderingService: RevurderingService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val personIdenter = objectMapper.readValue<PayloadOpprettAutomatiskRevurderingFraForvaltningTask>(task.payload).personIdenter
        val identerForAutomatiskRevurdering =
            personIdenter.filter { personIdent ->
                automatiskRevurderingService.kanAutomatiskRevurderes(personIdent)
            }

        secureLogger.info("Kan revurdere personIdenter: $identerForAutomatiskRevurdering - oppretter task for disse")

        if (identerForAutomatiskRevurdering.isNotEmpty()) {
            revurderingService.opprettAutomatiskInntektsendringTask(identerForAutomatiskRevurdering)
        }
    }

    companion object {
        const val TYPE = "opprettAutomatiskRevurderingFraForvaltningTask"

        fun opprettTask(payload: PayloadOpprettAutomatiskRevurderingFraForvaltningTask): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
            )
    }
}

data class PayloadOpprettAutomatiskRevurderingFraForvaltningTask(
    val personIdenter: List<String>,
    val årMåned: YearMonth,
)
