package no.nav.familie.ef.sak.behandling.oppgavekontroll

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.IsoFields

@Service
class BehandlingsoppgaveService(
    val taskService: TaskService,
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
    val oppgaveService: OppgaveService,
) {
    private val logger = Logg.getLogger(this::class)

    @Transactional
    fun opprettTask() {
        if (LeaderClient.isLeader() == true) {
            val ukenummer = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val year = LocalDate.now().year
            val payloadOgUnikNøkkel = "År:$year Uke:$ukenummer"
            val finnesTask = taskService.finnTaskMedPayloadOgType(payloadOgUnikNøkkel, BehandlingUtenOppgaveTask.TYPE)
            if (finnesTask == null) {
                logger.info("Oppretter finnBehandlingUtenOppgave-task, da den ikke finnes fra før")
                val task = BehandlingUtenOppgaveTask.opprettTask(payloadOgUnikNøkkel)
                taskService.save(task)
            }
        }
    }

    fun antallÅpneBehandlingerUtenOppgave(): Int {
        val gamleBehandlinger = finnAlleBehandlingerOpprettetForMerEnnTreUkerSiden()

        val eksternFagsakIds =
            gamleBehandlinger.map { fagsakService.hentFagsakForBehandling(it.id).eksternId.toString() }

        val opprettetTomTidspunktPåBehandleSakOppgave = LocalDateTime.now().minusWeeks(2).minusDays(5)
        val alleOppgaver = oppgaveService.finnBehandleSakOppgaver(opprettetTomTidspunktPåBehandleSakOppgave)
        val oppgaveSaksreferanser: List<String> = alleOppgaver.flatMap { it.oppgaver.mapNotNull { oppgave -> oppgave.saksreferanse } }

        val fagsakerMedÅpenBehandlingSomManglerOppgave = eksternFagsakIds.filterNot { oppgaveSaksreferanser.contains(it) }

        loggManglerOppgave(fagsakerMedÅpenBehandlingSomManglerOppgave)

        return fagsakerMedÅpenBehandlingSomManglerOppgave.size
    }

    private fun loggManglerOppgave(fagsakerMedÅpenBehandlingSomManglerOppgave: List<String>) {
        logger.info("Antall fagsaker med åpen behandling uten tilhørende oppgave: ${fagsakerMedÅpenBehandlingSomManglerOppgave.size}")
        fagsakerMedÅpenBehandlingSomManglerOppgave.forEach {
            logger.warn("Ekstern-fagsak-id for fagsak med åpen behandling uten tilhørende oppgave: $it")
        }
    }

    private fun finnAlleBehandlingerOpprettetForMerEnnTreUkerSiden(): List<Behandling> {
        val stønadstyper = listOf(StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER, StønadType.BARNETILSYN)
        val treUkerSiden = LocalDateTime.now().minusWeeks(3)
        val gamleBehandlinger =
            stønadstyper.flatMap { stønadstype ->
                behandlingService.hentUferdigeBehandlingerOpprettetFørDato(stønadstype, treUkerSiden)
            }
        return gamleBehandlinger
    }
}
