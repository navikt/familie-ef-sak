package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.VentePåStatusFraIverksettSteg
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = PollStatusFraIverksettTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Sjekker status på iverksetting av behandling.",
)
class PollStatusFraIverksettTask(
    private val ventePåStatusFraIverksettSteg: VentePåStatusFraIverksettSteg,
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)

        stegService.håndterSteg(behandling, ventePåStatusFraIverksettSteg, null)
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
            ).copy(triggerTid = LocalDateTime.now().plusSeconds(31))

        const val TYPE = "pollerStatusFraIverksett"
    }
}
