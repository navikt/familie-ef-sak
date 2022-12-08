package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTerminbarnTask.TYPE,
    beskrivelse = "Oppretter oppgave for terminbarn som ikke er født"
)
class OpprettOppgaveTerminbarnTask(
    val taskService: TaskService,
    val opprettOppgaverForTerminbarnService: OpprettOppgaverTerminbarnService,
    val featureToggleService: FeatureToggleService
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        if (featureToggleService.isEnabled(Toggle.OPPRETT_OPPGAVER_TERMINBARN)) {
            val oppgaveForBarn = objectMapper.readValue<OppgaveForBarn>(task.payload)
            opprettOppgaverForTerminbarnService.opprettOppgaveForTerminbarn(oppgaveForBarn)
        } else {
            logger.warn("Feature toggle opprett-oppgaver-ugyldigeterminbarn er ikke enablet. TaskID=${task.id}")
        }
    }

    companion object {
        fun opprettTask(opprettOppgavePayload: OppgaveForBarn): Task {
            return Task(TYPE, objectMapper.writeValueAsString(opprettOppgavePayload)).apply {
                this.metadata[MDCConstants.MDC_CALL_ID] = IdUtils.generateId()
            }
        }
        const val TYPE = "opprettOppgaveTerminbarnTask"
    }
}
