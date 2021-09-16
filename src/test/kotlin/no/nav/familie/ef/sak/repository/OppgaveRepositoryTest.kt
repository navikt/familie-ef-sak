package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
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

    @Test
    internal fun `skal finne nyeste oppgave for behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 2))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 3))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 4))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNotNull()
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id).gsakOppgaveId).isEqualTo(4)

    }

    @Test
    internal fun `skal finne nyeste oppgave for riktig behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))

        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling2, erFerdigstilt = true, gsakOppgaveId = 2))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNotNull()
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id).gsakOppgaveId).isEqualTo(1)

    }

    @Test
    internal fun `skal feile hvis det ikke finnes en oppgave for behandlingen`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThrows<EmptyResultDataAccessException> {
            oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)
        }

    }

}
