package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.beregning.OmregningService
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.kontrakter.felles.jsonMapper
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
import java.time.YearMonth
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = GOmregningTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "G-omregning",
)
class GOmregningTask(
    private val omregningService: OmregningService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val fagsakId = jsonMapper.readValue<GOmregningPayload>(task.payload).fagsakId
        omregningService.utførGOmregning(fagsakId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettTask(fagsakId: UUID): Boolean {
        val payload = jsonMapper.writeValueAsString(GOmregningPayload(fagsakId, Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed))
        val eksisterendeTask = taskService.finnTaskMedPayloadOgType(payload, TYPE)

        if (eksisterendeTask != null) {
            return false
        }

        val properties =
            Properties().apply {
                setProperty("fagsakId", fagsakId.toString())
                setProperty("grunnbeløpsdato", Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.toString())
                setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
            }

        val task = Task(TYPE, payload).copy(metadataWrapper = PropertiesWrapper(properties))

        taskService.save(task)
        return true
    }

    companion object {
        const val TYPE = "G-omregning"
    }
}

data class GOmregningPayload(
    val fagsakId: UUID,
    val nyesteGrunnbeløpGyldigFraOgMed: YearMonth,
)
