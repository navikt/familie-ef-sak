package no.nav.familie.ef.sak.behandling.grunnbelop

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.beregning.DryRunException
import no.nav.familie.ef.sak.beregning.OmregningService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = GOmregningTask.TYPE,
                     maxAntallFeil = 3,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "G-omregning")
class GOmregningTask(private val omregningService: OmregningService,
                     private val featureToggleService: FeatureToggleService) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)


    override fun doTask(task: Task) {
        val behandlingId = objectMapper.readValue<GOmregningTaskPayload>(task.payload).behandlingId
        try {
            omregningService.utførGOmregning(behandlingId,
                                             featureToggleService.isEnabled("familie.ef.sak.omberegning.live.run"))
        } catch (e: DryRunException) {
            logger.info("G-OmberegningTask for behandlingId $behandlingId ruller tilbake fordi den er kjørt i dry run-modus.")
        }
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

data class GOmregningTaskPayload(val behandlingId: UUID)