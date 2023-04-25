package no.nav.familie.ef.sak.behandling.oppgavekontroll

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Profile("!integrasjonstest")
@Service
class ÅpneBehandlingerUtenOppgaveScheduler(val behandlingService: BehandlingService, val fagsakService: FagsakService, val oppgaveService: OppgaveService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    //@Scheduled(cron = "\${G_OMREGNING_CRON_EXPRESSION}")
    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L)
    fun opprettGOmregningTaskForBehandlingerMedUtdatertG() {
        val stønadstyper = listOf(StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER, StønadType.BARNETILSYN)
        val toUkerSiden = LocalDateTime.now().minusWeeks(2)
        val gamleBehandlinger = stønadstyper.flatMap { stønadstype ->
            behandlingService.hentGamleUferdigeBehandlinger(stønadstype, toUkerSiden)
        }

        val eksternFagsakIds = gamleBehandlinger.map { fagsakService.hentFagsakForBehandling(it.id).eksternId.id.toString() }

        val fagsakerMedÅpenBehandlingSomManglerOppgave: List<String> = oppgaveService.finnFagsakerSomManglerOppgave(eksternFagsakIds)

        logger.info("Fagsak med åpen behandling uten oppgave antall ${fagsakerMedÅpenBehandlingSomManglerOppgave.size}")
        fagsakerMedÅpenBehandlingSomManglerOppgave.forEach {
            logger.info("Fagsak med åpen behandling uten oppgave: $it")
        }
    }
}