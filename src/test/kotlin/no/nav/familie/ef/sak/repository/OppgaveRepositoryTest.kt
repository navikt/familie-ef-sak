package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("local", "mock-oauth")
internal class OppgaveRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var customRepository: CustomRepository
    @Autowired private lateinit var oppgaveRepository: OppgaveRepository

    @Test
    internal fun `findByBehandlingIdAndType`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak))
        val oppgave = customRepository.persist(oppgave(behandling))

        assertThat(oppgaveRepository.findByBehandlingIdAndType(UUID.randomUUID(), Oppgavetype.BehandleSak)).isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndType(behandling.id, Oppgavetype.BehandleSak)).isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndType(behandling.id, oppgave.type)).isEqualTo(oppgave)
    }

    @Test
    internal fun `findByBehandlingIdAndTypeAndErFerdigstiltIsFalse`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak))
        val oppgave = customRepository.persist(oppgave(behandling, erFerdigstilt = true))

        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(UUID.randomUUID(), Oppgavetype.BehandleSak))
                .isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, Oppgavetype.BehandleSak))
                .isNull()
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, oppgave.type))
                .isNull()

        val oppgaveIkkeFerdigstilt = customRepository.persist(oppgave(behandling))
        assertThat(oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, oppgave.type))
                .isEqualTo(oppgaveIkkeFerdigstilt)
    }

}
