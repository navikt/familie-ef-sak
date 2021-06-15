package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = PollStatusFraIverksettTask.TYPE,
                     maxAntallFeil = 50,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Sjekker status på iverksetting av behandling.")

class PollStatusFraIverksettTask(private val stegService: StegService,
                                 private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        stegService.håndterPollStatusFraIverksett(behandling)
    }

    companion object {

        fun opprettTask(behandlingId: UUID): Task =
                Task(type = TYPE,
                     payload = behandlingId.toString(),
                     triggerTid = LocalDateTime.now().plusMinutes(18))


        const val TYPE = "pollerStatusFraIverksett"
    }


}