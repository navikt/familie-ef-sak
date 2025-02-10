package no.nav.familie.ef.sak.behandlingsflyt.task

import no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling.OppgaverForFerdigstillingService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillFremleggsoppgaverTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Skal ferdigstille valgte fremleggsoppgaver når behandlingen er besluttet.",
)
class FerdigstillFremleggsoppgaverTask(
    private val oppgaverForFerdigstillingService: OppgaverForFerdigstillingService,
    private val oppgaveService: OppgaveService,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)

        if (toggleKanFerdigstilleFremlegssoppgaver()) {
            val oppgaverForFerdigstilling = oppgaverForFerdigstillingService.hentOppgaverForFerdigstillingEllerNull(behandlingId)
            oppgaverForFerdigstilling?.fremleggsoppgaveIderSomSkalFerdigstilles?.forEach { id ->
                if (!erOppgaveFerdigstiltEllerFeilregistrert(id)) {
                    oppgaveService.ferdigstillOppgave(id)
                }
            }
        }
    }

    companion object {
        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingId.toString()
                    },
            )

        const val TYPE = "ferdigstillFremleggsoppgaverTask"
    }

    private fun erOppgaveFerdigstiltEllerFeilregistrert(oppgaveId: Long): Boolean {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        return oppgave.status == StatusEnum.FERDIGSTILT || oppgave.status == StatusEnum.FEILREGISTRERT
    }

    private fun toggleKanFerdigstilleFremlegssoppgaver(): Boolean = featureToggleService.isEnabled(Toggle.FRONTEND_VIS_MARKERE_GODKJENNE_OPPGAVE_MODAL)
}
