package no.nav.familie.ef.sak.karakterutskrift

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Year
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = KarakterutskriftBrevTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Automatisk utsendt brev for innhenting av karakterutskrift",
)
class KarakterutskriftBrevTask : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        // finn person/fagsak

        // valider har fagsak med løpende behandling?

        // opprett brev

        // journfalfør brev

        // distribuer brev

        // oppdater oppgave
    }

    companion object {

        fun opprettTask(fagsakId: UUID, karakterutskriftBrevtype: KarakterutskriftBrevtype, år: Year): Task {
            val payload = objectMapper.writeValueAsString(
                AutomatiskBrevKarakterutskriftPayload(
                    fagsakId,
                    karakterutskriftBrevtype,
                    år,
                ),
            )

            val properties = Properties().apply {
                setProperty("fagsakId", fagsakId.toString())
                setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
            }

            return Task(TYPE, payload, properties)
        }

        const val TYPE = "KarakterutskriftBrev"
    }
}

data class AutomatiskBrevKarakterutskriftPayload(
    val fagsakId: UUID,
    val brevtype: KarakterutskriftBrevtype,
    val år: Year,
)

enum class KarakterutskriftBrevtype {
    HOVEDPERIODE,
    UTVIDET,
}
