package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

/**
 * @param uniktId settes då payload er unikt på en task og ønsker å kunne opprette en ny task for samme ident, då disse taskene
 * settes til OK for å ikke spamme med feilede tasker for migrering hvis det er en eller annen MigreringException
 */
data class AutomatiskMigreringTaskData(
    val personIdent: String,
    val uniktId: UUID = UUID.randomUUID(),
)

@Service
@TaskStepBeskrivelse(
    taskStepType = AutomatiskMigreringTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Automatisk migrering",
)
class AutomatiskMigreringTask(
    private val automatiskMigreringService: AutomatiskMigreringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val personIdent = jsonMapper.readValue<AutomatiskMigreringTaskData>(task.payload).personIdent
        automatiskMigreringService.migrerPersonAutomatisk(personIdent)
    }

    companion object {
        const val TYPE = "automatiskMigrering"

        fun opprettTask(ident: String): Task =
            Task(
                TYPE,
                jsonMapper.writeValueAsString(AutomatiskMigreringTaskData(ident)),
                Properties().apply {
                    this["personIdent"] = ident
                },
            )
    }
}
