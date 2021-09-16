package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = PollStatusTekniskOpphør.TYPE,
                     maxAntallFeil = 50,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Sjekker status på teknisk opphør av behandling.")

class PollStatusTekniskOpphør(private val stegService: StegService,
                              private val behandlingService: BehandlingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingService.hentBehandling(behandlingId)
        stegService.håndterPollStatusTekniskOpphør(behandling)
    }

    companion object {

        fun opprettTask(behandlingId: UUID, personIdent: String): Task =
                Task(type = TYPE,
                     payload = behandlingId.toString(),
                     properties = Properties().apply {
                         this["behandlingId"] = behandlingId.toString()
                         this["personIdent"] = personIdent
                         this["saksbehandler"] = SikkerhetContext.hentSaksbehandler(strict = true)
                     }).copy(triggerTid = LocalDateTime.now().plusMinutes(5))


        const val TYPE = "pollerStatusTekniskOpphør"
    }


}