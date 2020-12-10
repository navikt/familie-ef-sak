package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.*


data class StatusPåOppdragTaskPayload(val behandlingId: UUID)

@Service
@TaskStepBeskrivelse(taskStepType = StatusPåOppdragTask.TYPE,
                     maxAntallFeil = 50,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15*60L,
                     beskrivelse = "Sjekker status på utbetalningsoppdraget mot økonomi.")

class StatusPåOppdragTask(private val stegService: StegService,
                          private val taskRepository: TaskRepository,
                          private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)

        stegService.håndterStatusPåOppdrag(behandling)
    }


    override fun onCompletion(task: Task) {
        //TODO Skall sende ut vedtaksbrev?
    }

    companion object {

        const val TYPE = "sjekkStatusPåOppdrag"
    }


}