package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = JournalførVedtaksbrevTask.TYPE,
                     maxAntallFeil = 50,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Journalfører vedtaksbrev.")

class JournalførVedtaksbrevTask(private val stegService: StegService,
                                private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        stegService.håndterJournalførVedtaksbrev(behandling)
    }

    companion object {
        fun opprettTask(behandling: Behandling): Task =
                Task(type = TYPE,
                     payload = behandling.id.toString(),
                     triggerTid = LocalDateTime.now().plusSeconds(30))

        const val TYPE = "journalførVedtaksbrev"
    }


}