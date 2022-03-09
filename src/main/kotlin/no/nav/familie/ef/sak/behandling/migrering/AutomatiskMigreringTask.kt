package no.nav.familie.ef.sak.behandling.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

data class AutomatiskMigreringTaskData(val identer: Set<String>)

@Service
@TaskStepBeskrivelse(taskStepType = AutomatiskMigreringTask.TYPE,
                     maxAntallFeil = 3,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Automatisk migrering")
class AutomatiskMigreringTask(private val automatiskMigreringService: AutomatiskMigreringService): AsyncTaskStep {

    override fun doTask(task: Task) {
        val identer = objectMapper.readValue<AutomatiskMigreringTaskData>(task.payload).identer
        automatiskMigreringService.migrerAutomatisk(identer)
    }

    companion object {
        const val TYPE = "automatiskMigrering"

        fun opprettTask(identer: Set<String>): Task {
            return Task(TYPE, objectMapper.writeValueAsString(AutomatiskMigreringTaskData(identer)))
        }
    }
}