package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegServiceDeprecated
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagSaksbehandlingsblankettTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Lag blankett for å dokumentere saksbehandling (fallback).",
)
class LagSaksbehandlingsblankettTask(
    private val stegServiceDeprecated: StegServiceDeprecated,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val saksbehandling = behandlingService.hentSaksbehandling(UUID.fromString(task.payload))
        stegServiceDeprecated.håndterLagSaksbehandlingsblankett(saksbehandling)
    }

    companion object {
        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingId.toString()
                    },
            )

        const val TYPE = "lagSaksbehandlingsblankettTask"
    }
}
