package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = IverksettMotOppdragTask.TYPE,
                     beskrivelse = "Utfører iverksetting av utbetalning mot økonomi.")
class IverksettMotOppdragTask(
        private val stegService: StegService,
        private val behandlingService: BehandlingService,
        private val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        stegService.håndterIverksettingOppdrag(behandling)
    }

    companion object {

        const val TYPE = "utførIverksettingAvUtbetalning"
    }


}