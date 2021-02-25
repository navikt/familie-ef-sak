package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = JournalførBlankettTask.TYPE,
                     beskrivelse = "Utfører journalføring av saksbehandlingsblankett")
class JournalførBlankettTask(private val stegService: StegService,
                             private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        stegService.håndterBlankett(behandling)
    }

    companion object {

        fun opprettTask(behandling: Behandling, personIdent: String): Task {
            return Task(type = TYPE,
                        payload = behandling.id.toString(),
                        properties = Properties().apply {
                            this["personIdent"] = personIdent
                            this["behandlingId"] = behandling.id.toString()
                            this["saksbehandler"] = SikkerhetContext.hentSaksbehandler()
                        })
        }

        const val TYPE = "utførJournalførBlankett"
    }
}