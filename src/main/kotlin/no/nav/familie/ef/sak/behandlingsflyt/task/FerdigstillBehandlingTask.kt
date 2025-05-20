package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegServiceDeprecated
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillBehandlingTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Ferdigstill behandling.",
)
class FerdigstillBehandlingTask(
    private val stegServiceDeprecated: StegServiceDeprecated,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        stegServiceDeprecated.håndterFerdigstillBehandling(behandling)
    }

    companion object {
        fun opprettTask(saksbehandling: Saksbehandling): Task =
            Task(
                type = TYPE,
                payload = saksbehandling.id.toString(),
                properties =
                    Properties().apply {
                        this["behandlingId"] = saksbehandling.id.toString()
                    },
            )

        const val TYPE = "ferdigstillBehandling"
    }
}
