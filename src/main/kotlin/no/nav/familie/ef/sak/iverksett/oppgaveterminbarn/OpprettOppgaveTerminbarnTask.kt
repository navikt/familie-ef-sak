package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTerminbarnTask.TYPE,
    beskrivelse = "Oppretter oppgave for terminbarn som ikke er født",
)
class OpprettOppgaveTerminbarnTask(
    val taskService: TaskService,
    val opprettOppgaverForTerminbarnService: OpprettOppgaverTerminbarnService,
    val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val oppgaveForBarn = jsonMapper.readValue<OppgaveForBarn>(task.payload)
        opprettOppgaverForTerminbarnService.opprettOppgaveForTerminbarn(oppgaveForBarn)
    }

    companion object {
        fun opprettTask(opprettOppgavePayload: OppgaveForBarn): Task =
            Task(TYPE, jsonMapper.writeValueAsString(opprettOppgavePayload)).apply {
                this.metadata[MDCConstants.MDC_CALL_ID] = IdUtils.generateId()
            }

        const val TYPE = "opprettOppgaveTerminbarnTask"
    }
}
