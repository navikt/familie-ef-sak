package no.nav.familie.ef.sak.behandling.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class AutomatiskMigreringTaskData(val personIdenter: Set<String>)

@Service
@TaskStepBeskrivelse(taskStepType = AutomatiskMigreringTask.TYPE,
                     maxAntallFeil = 3,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Automatisk migrering")
class AutomatiskMigreringTask(private val automatiskMigreringService: AutomatiskMigreringService) : AsyncTaskStep {

    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        val personIdenter = objectMapper.readValue<AutomatiskMigreringTaskData>(task.payload).personIdenter
        var antallFeil = 0
        personIdenter.forEach { personIdent ->
            try {
                automatiskMigreringService.migrerPersonAutomatisk(personIdent)
            } catch (e: Exception) {
                secureLogger.warn("Feilet migrering av $personIdent ${e.message}", e)
                antallFeil++
            }
        }
        if (antallFeil > 0) {
            error("Feilet $antallFeil migreringer, sjekk securelogs for mer info")
        }
    }

    companion object {

        const val TYPE = "automatiskMigrering"

        fun opprettTask(identer: Set<String>): Task {
            return Task(TYPE, objectMapper.writeValueAsString(AutomatiskMigreringTaskData(identer)))
        }
    }
}