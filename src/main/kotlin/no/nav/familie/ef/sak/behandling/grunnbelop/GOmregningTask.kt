package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.DryRunException
import no.nav.familie.ef.sak.beregning.OmregningService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = GOmregningTask.TYPE,
                     maxAntallFeil = 1,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "G-omregning")
class GOmregningTask(private val omregningService: OmregningService,
                     private val featureToggleService: FeatureToggleService) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)


    override fun doTask(task: Task) {
        val fagsakId = UUID.fromString(task.payload)
        try {
            omregningService.utførGOmregning(fagsakId,
                                             featureToggleService.isEnabled("familie.ef.sak.omberegning.live.run"))
        } catch (e: DryRunException) {
            logger.info("G-OmberegningTask for fagsakId $fagsakId ruller tilbake fordi den er kjørt i dry run-modus.")
        }
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
