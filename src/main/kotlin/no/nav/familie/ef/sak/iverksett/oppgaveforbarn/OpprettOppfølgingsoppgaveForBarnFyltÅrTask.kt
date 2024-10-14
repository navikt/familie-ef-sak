package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppfølgingsoppgaveForBarnFyltÅrTask.TYPE,
    beskrivelse = "Oppretter oppfølgingsoppgave for barn som har fylt 1/2 eller 1 år",
)
class OpprettOppfølgingsoppgaveForBarnFyltÅrTask(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(task.payload)
        val opprettetOppgaveId =
            oppgaveService.opprettOppgaveUtenÅLagreIRepository(
                opprettOppgavePayload.behandlingId,
                Oppgavetype.InnhentDokumentasjon,
                opprettOppgavePayload.aktivFra,
                opprettOppgavePayload.alder.oppgavebeskrivelse,
                tilordnetNavIdent = null,
            )
        val oppgave =
            Oppgave(
                gsakOppgaveId = opprettetOppgaveId,
                behandlingId = opprettOppgavePayload.behandlingId,
                barnPersonIdent = opprettOppgavePayload.barnPersonIdent,
                type = Oppgavetype.InnhentDokumentasjon,
                alder = opprettOppgavePayload.alder,
            )
        oppgaveRepository.insert(oppgave)
    }

    companion object {
        const val TYPE = "opprettOppfølgingsoppgaveForBarnFyltÅrTask"

        fun opprettTask(opprettOppgavePayload: OpprettOppgavePayload): Task =
            Task(TYPE, objectMapper.writeValueAsString(opprettOppgavePayload)).apply {
                this.metadata[MDCConstants.MDC_CALL_ID] = IdUtils.generateId()
            }
    }
}

data class OpprettOppgavePayload(
    val behandlingId: UUID,
    val barnPersonIdent: String,
    val søkerPersonIdent: String,
    val alder: AktivitetspliktigAlder,
    val aktivFra: LocalDate? = null,
)
