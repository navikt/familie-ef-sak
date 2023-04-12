package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = PubliserVedtakshendelseTask.TYPE,
    beskrivelse = "Sender hendelse om vedtak",
)
class PubliserVedtakshendelseTask(private val stegService: StegService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        stegService.publiserVedtakshendelse(UUID.fromString(task.payload))
    }

    companion object {

        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties = Properties().apply {
                    this["behandlingId"] = behandlingId.toString()
                },
            )

        const val TYPE = "publiserVedtakshendelse"
    }
}
