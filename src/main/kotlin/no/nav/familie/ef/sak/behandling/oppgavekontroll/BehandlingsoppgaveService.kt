package no.nav.familie.ef.sak.behandling.oppgavekontroll

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class BehandlingsoppgaveService(
    val taskService: TaskService,
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
    val oppgaveService: OppgaveService,
    val featureToggleService: FeatureToggleService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun opprettTask() {
        Thread.sleep(Random.nextLong(5_000)) // YOLO unngå feil med att 2 samtidige podder oppretter task
        val finnesTask =
            taskService.finnTaskMedPayloadOgType("finnBehandlingUtenOppgave", BehandlingUtenOppgaveTask.TYPE)
        if (finnesTask == null) {
            logger.info("Oppretter satsendring-task, da den ikke finnes fra før")
            val task = BehandlingUtenOppgaveTask.opprettTask()
            taskService.save(task)
        }
    }

    fun validerHarIkkeÅpneBehandligerUtenOppgave() {
        val stønadstyper = listOf(StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER, StønadType.BARNETILSYN)
        val toUkerSiden = LocalDateTime.now().minusWeeks(2)
        val gamleBehandlinger = stønadstyper.flatMap { stønadstype ->
            behandlingService.hentGamleUferdigeBehandlinger(stønadstype, toUkerSiden)
        }

        val eksternFagsakIds =
            gamleBehandlinger.map { fagsakService.hentFagsakForBehandling(it.id).eksternId.id.toString() }

        val alleOppgaver = oppgaveService.finnBehandleSakOppgaver()
        val oppgaveSaksreferanser: List<String> = alleOppgaver.flatMap { it.oppgaver.mapNotNull { oppgave -> oppgave.saksreferanse } }
        val fagsakerMedÅpenBehandlingSomManglerOppgave = eksternFagsakIds.filterNot { oppgaveSaksreferanser.contains(it) }

        logger.info("Fagsak med åpen behandling uten oppgave antall ${fagsakerMedÅpenBehandlingSomManglerOppgave.size}")
        fagsakerMedÅpenBehandlingSomManglerOppgave.forEach {
            logger.warn("Fagsaker med åpen behandling uten oppgave: $it")
        }

        val enabled = featureToggleService.isEnabled(Toggle.KAST_FEIL_HVIS_OPPGAVE_MANGLER_PÅ_ÅPEN_BEHANDLING)
        val harFunnetFagsakUtenOppgave = fagsakerMedÅpenBehandlingSomManglerOppgave.size > 0
        feilHvis(enabled && harFunnetFagsakUtenOppgave){"Åpne behandlinger uten behandleSak oppgave funnet på fagsak "}

    }
}
