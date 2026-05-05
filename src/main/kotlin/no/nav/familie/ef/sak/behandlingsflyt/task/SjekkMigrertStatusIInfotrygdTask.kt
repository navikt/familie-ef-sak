package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandling.migrering.MigreringService
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Properties
import java.util.UUID

/**
 * Brukes som ett mellomsteg før LagSaksbehandlingsblankettTask for å sjekke att personen har blitt opphørt i Infotrygd
 * Kjører i 16 min før den settes til feilet
 */
@Service
@TaskStepBeskrivelse(
    taskStepType = SjekkMigrertStatusIInfotrygdTask.TYPE,
    maxAntallFeil = 7,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 120L,
    beskrivelse = "Sjekker status for migrert sak",
)
class SjekkMigrertStatusIInfotrygdTask(
    private val migreringService: MigreringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val (behandlingId, opphørsmåned) = jsonMapper.readValue<SjekkMigrertStatusIInfotrygdData>(task.payload)

        if (!migreringService.erOpphørtIInfotrygd(behandlingId, opphørsmåned)) {
            throw TaskExceptionUtenStackTrace("Er ikke opphørt i infotrygd")
        }
    }

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            opphørsmåned: YearMonth,
            personIdent: String,
        ): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(SjekkMigrertStatusIInfotrygdData(behandlingId, opphørsmåned)),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingId.toString()
                        this["personIdent"] = personIdent
                    },
            ).copy(triggerTid = LocalDateTime.now().plusMinutes(16))

        const val TYPE = "sjekkMigrertStatus"
    }

    data class SjekkMigrertStatusIInfotrygdData(
        val behandlingId: UUID,
        val opphørsmåned: YearMonth,
    )
}
