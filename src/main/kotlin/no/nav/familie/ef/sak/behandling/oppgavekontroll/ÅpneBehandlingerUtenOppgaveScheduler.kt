package no.nav.familie.ef.sak.behandling.oppgavekontroll

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Profile("!integrasjonstest")
@Service
class ÅpneBehandlingerUtenOppgaveScheduler(val behandlingsoppgaveService: BehandlingsoppgaveService) {

    @Scheduled(cron = "\${FINN_BEHANDLINGER_UTEN_OPPGAVE_CRON_EXPRESSION}")
    @Transactional
    fun opprettTaskFinnÅpneBehandlingerUtenOppgave() {
        behandlingsoppgaveService.opprettTask()
    }
}
