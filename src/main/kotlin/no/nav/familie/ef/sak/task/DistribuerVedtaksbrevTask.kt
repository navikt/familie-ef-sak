package no.nav.familie.ef.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = DistribuerVedtaksbrevTask.TYPE,
                     maxAntallFeil = 50,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Distribuerer vedtaksbrev.")

class DistribuerVedtaksbrevTask(private val stegService: StegService,
                                private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue<DistribuerVedtaksbrevTaskData>(task.payload)
        val behandling = behandlingService.hentBehandling(taskData.behandlingId)
        stegService.håndterDistribuerVedtaksbrev(behandling, taskData.journalpostId)
    }

    companion object {

        fun opprettTask(behandling: Behandling, journalpostId: String): Task =
                Task(type = TYPE,
                     payload = objectMapper.writeValueAsString(DistribuerVedtaksbrevTaskData(behandlingId = behandling.id,
                                                                                             journalpostId = journalpostId)),
                     triggerTid = LocalDateTime.now().plusSeconds(30))

        const val TYPE = "distribuerVedtaksbrev"
    }

    data class DistribuerVedtaksbrevTaskData(val behandlingId: UUID, val journalpostId: String)

}