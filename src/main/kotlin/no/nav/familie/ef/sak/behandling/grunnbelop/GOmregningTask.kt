package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.OmregningService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = GOmregningTask.TYPE,
                     maxAntallFeil = 1,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "G-omregning")
class GOmregningTask(private val omregningService: OmregningService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val fagsakId = UUID.fromString(task.payload)
        omregningService.utførGOmregning(fagsakId)
    }

    companion object {

        const val TYPE = "G-omregning"

        fun opprettTask(fagsakId: UUID): Task {
            return Task(TYPE, fagsakId.toString())
        }

        fun opprettTasks(fagsakIder: List<UUID>): List<Task> {
            return fagsakIder.map { opprettTask(it) }
        }
    }
}
