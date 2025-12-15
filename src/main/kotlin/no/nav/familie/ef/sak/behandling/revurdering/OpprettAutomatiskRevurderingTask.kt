package no.nav.familie.ef.sak.behandling.revurdering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.YearMonth
import kotlin.collections.set

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettAutomatiskRevurderingTask.TYPE,
    beskrivelse = "Automatisk revurdering av inntekt - Brukes i månedlig kjøring av inntektskontroll",
)
class OpprettAutomatiskRevurderingTask(
    private val automatiskRevurderingService: AutomatiskRevurderingService,
    private val revurderingService: RevurderingService,
) : AsyncTaskStep {
    private val logger = Logg.getLogger(this::class)

    override fun doTask(task: Task) {
        val personIdenter = objectMapper.readValue<PayloadOpprettAutomatiskRevurderingTask>(task.payload).personIdenter
        val identerForAutomatiskRevurdering =
            personIdenter.filter { personIdent ->
                automatiskRevurderingService.kanAutomatiskRevurderes(personIdent)
            }

        logger.info("Kan revurdere personIdenter: $identerForAutomatiskRevurdering - oppretter task for disse")

        if (identerForAutomatiskRevurdering.isNotEmpty()) {
            revurderingService.opprettAutomatiskInntektsendringTask(identerForAutomatiskRevurdering)
        }
    }

    companion object {
        const val TYPE = "opprettAutomatiskRevurderingTask"

        fun opprettTask(payload: PayloadOpprettAutomatiskRevurderingTask): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(payload),
            )
    }
}

data class PayloadOpprettAutomatiskRevurderingTask(
    val personIdenter: List<String>,
    val yearMonth: YearMonth,
)
