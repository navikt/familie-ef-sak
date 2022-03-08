package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = JournalførBlankettTask.TYPE,
                     beskrivelse = "Utfører journalføring av saksbehandlingsblankett")
class JournalførBlankettTask(private val stegService: StegService,
                             private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        stegService.håndterBlankett(behandling)
    }

    companion object {

        fun opprettTask(behandling: Saksbehandling): Task {
            return Task(type = TYPE,
                        payload = behandling.id.toString(),
                        properties = Properties().apply {
                            this["personIdent"] = behandling.ident
                            this["behandlingId"] = behandling.id.toString()
                            this["saksbehandler"] = SikkerhetContext.hentSaksbehandler()
                        })
        }

        const val TYPE = "utførJournalførBlankett"
    }
}