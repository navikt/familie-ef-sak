package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class Regelendring2026RepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var regelendring2026Repository: Regelendring2026Repository

    @Test
    internal fun `upsert skal opprette ny rad når den ikke finnes fra før`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        regelendring2026Repository.upsert(behandling.id, "Fordi")

        val lagret = regelendring2026Repository.findByBehandlingId(behandling.id)
        assertThat(lagret?.begrunnelse).isEqualTo("Fordi")
    }

    @Test
    internal fun `upsert skal oppdatere eksisterende rad uten å feile på duplicate key`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        regelendring2026Repository.upsert(behandling.id, "Første begrunnelse")
        regelendring2026Repository.upsert(behandling.id, "Oppdatert begrunnelse")

        val lagret = regelendring2026Repository.findByBehandlingId(behandling.id)
        assertThat(lagret?.begrunnelse).isEqualTo("Oppdatert begrunnelse")
    }
}
