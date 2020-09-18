package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("local", "mock-oauth")
internal class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var customRepository: CustomRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun `findByFagsakId`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak))

        assertThat(behandlingRepository.findByFagsakId(UUID.randomUUID())).isEmpty()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).containsOnly(behandling)
    }

    @Test
    internal fun `findByFagsakIdAndAktivIsTrue`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak, aktiv = false))

        assertThat(behandlingRepository.findByFagsakId(UUID.randomUUID())).isEmpty()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).isEmpty()

        val aktivBehandling = customRepository.persist(behandling(fagsak, aktiv = true))
        assertThat(behandlingRepository.findByFagsakId(fagsak.id)).containsOnly(aktivBehandling)
    }

    @Test
    internal fun `findByFagsakAndStatus`() {
        val fagsak = customRepository.persist(fagsak())
        val behandling = customRepository.persist(behandling(fagsak, status = BehandlingStatus.OPPRETTET))

        assertThat(behandlingRepository.findByFagsakIdAndStatus(UUID.randomUUID(), BehandlingStatus.OPPRETTET)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, BehandlingStatus.AVSLUTTET)).isEmpty()
        assertThat(behandlingRepository.findByFagsakIdAndStatus(fagsak.id, BehandlingStatus.OPPRETTET)).containsOnly(behandling)
    }

}