package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BarnetilsynSatsendringTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Barnetilsyn satsendring",
)
class BarnetilsynSatsendringTask(
    val barnetilsynSatsendringService: BarnetilsynSatsendringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        barnetilsynSatsendringService.finnFagsakerSomSkalSatsendresMedNySatsDersomBaselineErOk()
    }

    companion object {
        const val TYPE = "barnetilsynSatsendring"

        fun opprettTask(payload: String): Task =
            Task(
                TYPE,
                payload,
                Properties(),
            )
    }
}
