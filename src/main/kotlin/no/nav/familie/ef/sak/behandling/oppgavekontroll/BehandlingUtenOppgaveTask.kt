package no.nav.familie.ef.sak.behandling.oppgavekontroll

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingUtenOppgaveTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Finn åpne behandlinger uten behandle sak oppgave",
)
class BehandlingUtenOppgaveTask(val behandlingsoppgaveService: BehandlingsoppgaveService, val featureToggleService: FeatureToggleService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val antallÅpneBehandlingerUtenOppgave = behandlingsoppgaveService.antallÅpneBehandlingerUtenOppgave()
        val skalKasteFeilHvisOppgaveMangler = featureToggleService.isEnabled(Toggle.KAST_FEIL_HVIS_OPPGAVE_MANGLER_PÅ_ÅPEN_BEHANDLING)
        feilHvis(skalKasteFeilHvisOppgaveMangler && antallÅpneBehandlingerUtenOppgave > 0) { "Åpne behandlinger uten behandleSak oppgave funnet på fagsak " }
    }

    companion object {

        const val TYPE = "finnBehandlingUtenOppgave"

        fun opprettTask(ukenummer: Int): Task {
            return Task(
                TYPE,
                ukenummer.toString(),
                Properties(),
            )
        }
    }
}
