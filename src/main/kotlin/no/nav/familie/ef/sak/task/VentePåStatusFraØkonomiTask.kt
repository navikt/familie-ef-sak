package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = VentePåStatusFraØkonomiTask.TYPE,
                     maxAntallFeil = 50,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15*60L,
                     beskrivelse = "Sjekker status på utbetalningsoppdraget mot økonomi.")

class VentePåStatusFraØkonomiTask(private val stegService: StegService,
                          private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        stegService.håndterStatusPåOppdrag(behandling)
    }

    companion object {

        const val TYPE = "sjekkStatusPåOppdrag"
    }


}