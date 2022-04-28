package no.nav.familie.ef.sak.behandling.grunnbelop

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = GOmregningTask.TYPE,
                     maxAntallFeil = 3,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "G-omregning")
class GOmregningTask(private val gOmregningService: GOmregningService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = objectMapper.readValue<GOmregningTaskPayload>(task.payload).behandlingId
        gOmregningService.oppdaterBehandlingMedNyG(behandlingId)
    }

    companion object {

        const val TYPE = "G-omregning"

        fun opprettTask(behandlingId: UUID): Task {
            return Task(TYPE, objectMapper.writeValueAsString(GOmregningTaskPayload(behandlingId)), Properties().apply {
                this["behandlingId"] = behandlingId
            })
        }

        fun opprettTasks(behandlingIds: List<UUID>): List<Task> {
            return behandlingIds.map { opprettTask(it) }
        }
    }
}

data class GOmregningTaskPayload(
        val behandlingId: UUID
)