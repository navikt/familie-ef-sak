package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class OppgaveRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingIdAndTypeAndErFerdigstiltIsFalse() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
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
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val sporbar = Sporbar(opprettetTid = LocalDateTime.now().plusDays(1))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 2).copy(sporbar = sporbar))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 3))
        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 4))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNotNull
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)?.gsakOppgaveId)
            .isEqualTo(2)
    }

    @Test
    internal fun `skal finne nyeste oppgave for riktig behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))

        oppgaveRepository.insert(oppgave(behandling, erFerdigstilt = true, gsakOppgaveId = 1))
        oppgaveRepository.insert(oppgave(behandling2, erFerdigstilt = true, gsakOppgaveId = 2))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNotNull()
        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)?.gsakOppgaveId)
            .isEqualTo(1)
    }

    @Test
    internal fun `skal ikke feile hvis det ikke finnes en oppgave for behandlingen`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        assertThat(oppgaveRepository.findTopByBehandlingIdOrderBySporbarOpprettetTidDesc(behandling.id)).isNull()
    }
}
