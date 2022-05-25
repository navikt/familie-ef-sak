package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.DryRunException
import no.nav.familie.ef.sak.beregning.OmregningService
import no.nav.familie.ef.sak.beregning.nyesteGrunnbeløpGyldigFraOgMed
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = GOmregningTask.TYPE,
                     maxAntallFeil = 3,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "G-omregning")
class GOmregningTask(private val omregningService: OmregningService,
                     private val taskService: TaskService,
                     private val featureToggleService: FeatureToggleService) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)


    override fun doTask(task: Task) {
        val fagsakId = UUID.fromString(task.payload)
        try {
            omregningService.validerOgutførGOmregning(fagsakId,
                                                      featureToggleService.isEnabled("familie.ef.sak.omberegning.live.run"))
        } catch (e: DryRunException) {
            logger.info("G-OmberegningTask for fagsakId $fagsakId ruller tilbake fordi den er kjørt i dry run-modus.")
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettTask(fagsakId: UUID) {
        val eksisterendeTask = taskService.finnTaskMedPayloadOgType(fagsakId.toString(), TYPE)

        if (eksisterendeTask != null) {
            return
        }

        val properties = Properties().apply {
            setProperty("fagsakId", fagsakId.toString())
            setProperty("grunnbeløpsdato", nyesteGrunnbeløpGyldigFraOgMed.toString())
            setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
        }
        val task = Task(TYPE, fagsakId.toString()).copy(metadataWrapper = PropertiesWrapper(properties))

        taskService.save(task)
    }

    companion object {

        const val TYPE = "G-omregning"
    }
}
