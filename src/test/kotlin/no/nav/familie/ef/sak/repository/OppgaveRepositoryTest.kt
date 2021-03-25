package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

internal class OppgaveRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var oppgaveRepository: OppgaveRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val oppgave = oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true))

        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(UUID.randomUUID(), Oppgavetype.BehandleSak))
                .isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, Oppgavetype.BehandleSak))
                .isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, oppgave.type))
                .isNull()

        val oppgaveIkkeFerdigstilt = oppgaveRepository.insert(oppgave(behandling))
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, oppgave.type))
                .isEqualTo(oppgaveIkkeFerdigstilt)
    }

}
