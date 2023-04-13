package no.nav.familie.ef.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTask.TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for behandling",
    maxAntallFeil = 3,
)
class OpprettOppgaveTask(private val oppgaveService: OppgaveService, private val behandlingService: BehandlingService) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Då payload er unik per task type, så settes unik inn
     */
    data class OpprettOppgaveTaskData(
        val behandlingId: UUID,
        val oppgavetype: Oppgavetype,
        val tilordnetNavIdent: String? = null,
        val beskrivelse: String? = null,
        val unik: LocalDateTime? = LocalDateTime.now(),
    )

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(task.payload)
        val oppgavetype = data.oppgavetype

        if (oppgavetype == Oppgavetype.BehandleSak) {
            val behandling = behandlingService.hentBehandling(data.behandlingId)

            if (listOf(BehandlingStatus.FATTER_VEDTAK, BehandlingStatus.IVERKSETTER_VEDTAK, BehandlingStatus.FERDIGSTILT).contains(behandling.status)) {
                logger.info("Opprettet ikke oppgave med oppgavetype = $oppgavetype fordi status = ${behandling.status}")
                return
            }
        }

        val oppgaveId = oppgaveService.opprettOppgave(
            behandlingId = data.behandlingId,
            oppgavetype = oppgavetype,
            tilordnetNavIdent = data.tilordnetNavIdent,
            beskrivelse = data.beskrivelse,
        )

        task.metadata.setProperty("oppgaveId", oppgaveId.toString())
    }

    companion object {

        fun opprettTask(data: OpprettOppgaveTaskData): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(data),
                properties = Properties().apply {
                    this["saksbehandler"] = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
                    this["behandlingId"] = data.behandlingId.toString()
                    this["oppgavetype"] = data.oppgavetype.name
                },
            )
        }

        const val TYPE = "opprettOppgave"
    }
}
