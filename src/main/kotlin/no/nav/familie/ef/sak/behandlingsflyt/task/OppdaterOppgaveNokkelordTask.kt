package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterOppgaveNokkelordTask.TYPE,
    beskrivelse = "Oppdater nøkkelord på oppgave",
    maxAntallFeil = 3,
)
class OppdaterOppgaveNokkelordTask(
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    data class OppdaterOppgaveNokkelordTaskData(
        val oppgaveId: Long,
        val nokkelord: Set<String>,
        val unik: LocalDateTime = LocalDateTime.now(),
    )

    override fun doTask(task: Task) {
        val data = jsonMapper.readValue<OppdaterOppgaveNokkelordTaskData>(task.payload)
        oppgaveService.oppdaterOppgaveNokkelord(data.oppgaveId, data.nokkelord)
    }

    companion object {
        const val TYPE = "oppdaterOppgaveNokkelord"

        fun opprettTask(
            behandlingId: UUID,
            oppgaveId: Long,
            nokkelord: Set<String>,
        ): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(OppdaterOppgaveNokkelordTaskData(oppgaveId, nokkelord)),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
                        this["behandlingId"] = behandlingId.toString()
                        this["oppgaveId"] = oppgaveId.toString()
                    },
            )
    }
}
