package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveForOpprettetBehandlingTask.TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for opprettet revurdering",
    maxAntallFeil = 3,
)
class OpprettOppgaveForOpprettetBehandlingTask(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class OpprettOppgaveTaskData(
        val behandlingId: UUID,
        val saksbehandler: String,
        val beskrivelse: String? = null,
        val hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
        val mappeId: Long? = null,
        val prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    )

    override fun doTask(task: Task) {
        val data = jsonMapper.readValue<OpprettOppgaveTaskData>(task.payload)
        val behandlingId = data.behandlingId
        val oppgaveId = opprettOppgave(data, task)

        taskService.save(
            BehandlingsstatistikkTask.opprettMottattTask(
                behandlingId = behandlingId,
                hendelseTidspunkt = data.hendelseTidspunkt,
                oppgaveId = oppgaveId,
                saksbehandler = data.saksbehandler,
            ),
        )
    }

    private fun opprettOppgave(
        data: OpprettOppgaveTaskData,
        task: Task,
    ): Long? {
        val behandling = behandlingService.hentBehandling(data.behandlingId)
        if (behandling.status == BehandlingStatus.OPPRETTET || behandling.status == BehandlingStatus.UTREDES) {
            val tilordnetNavIdent =
                if (data.saksbehandler == SikkerhetContext.SYSTEM_FORKORTELSE) null else data.saksbehandler
            val oppgaveId =
                oppgaveService.opprettOppgave(
                    behandlingId = data.behandlingId,
                    oppgavetype = Oppgavetype.BehandleSak,
                    tilordnetNavIdent = tilordnetNavIdent,
                    beskrivelse = data.beskrivelse,
                    mappeId = data.mappeId,
                    prioritet = data.prioritet,
                )
            task.metadata.setProperty("oppgaveId", oppgaveId.toString())
            return oppgaveId
        } else {
            logger.warn("Oppretter ikke oppgave på behandling=${behandling.id} då den har status=${behandling.status}")
            return null
        }
    }

    companion object {
        fun opprettTask(data: OpprettOppgaveTaskData): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(data),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = data.saksbehandler
                        this["behandlingId"] = data.behandlingId.toString()
                    },
            )

        const val TYPE = "opprettOppgaveForOpprettetBehandling"
    }
}
