package no.nav.familie.ef.sak.no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.samværsavtale
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class SamværsavtaleRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var samværsavtaleRepository: SamværsavtaleRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandling(fagsak)
        val behandlingBarn = behandlingBarn(behandlingId = behandling.id, personIdent = "1")
        val avtale = samværsavtale(behandlingId = behandling.id, behandlingBarnid = behandlingBarn.id)

        behandlingRepository.insert(behandling)
        barnRepository.insert(behandlingBarn)
        samværsavtaleRepository.insert(avtale)

        val lagredeAvtaler = samværsavtaleRepository.findByBehandlingId(avtale.behandlingId)
        assertThat(lagredeAvtaler.size).isEqualTo(1)
        assertThat(lagredeAvtaler.first().behandlingId).isEqualTo(behandling.id)
        assertThat(lagredeAvtaler.first().behandlingBarnId).isEqualTo(behandlingBarn.id)
    }

    @Test
    internal fun findByBehandlingIdAndBehandlingBarnId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandling(fagsak)
        val behandlingBarn1 = behandlingBarn(behandlingId = behandling.id, personIdent = "1")
        val behandlingBarn2 = behandlingBarn(behandlingId = behandling.id, personIdent = "2")
        val avtale1 = samværsavtale(behandlingId = behandling.id, behandlingBarnid = behandlingBarn1.id)
        val avtale2 = samværsavtale(behandlingId = behandling.id, behandlingBarnid = behandlingBarn2.id)

        behandlingRepository.insert(behandling)
        barnRepository.insert(behandlingBarn1)
        barnRepository.insert(behandlingBarn2)
        samværsavtaleRepository.insert(avtale1)
        samværsavtaleRepository.insert(avtale2)

        val ikkeEksisterendeAvtale = samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandling.id, UUID.randomUUID())
        val lagretAvtale = samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandling.id, behandlingBarn1.id)

        assertThat(ikkeEksisterendeAvtale).isNull()
        assertThat(lagretAvtale).isNotNull
        assertThat(lagretAvtale?.behandlingId).isEqualTo(behandling.id)
        assertThat(lagretAvtale?.behandlingBarnId).isEqualTo(behandlingBarn1.id)
    }

    @Test
    internal fun deleteByBehandlingIdAndBehandlingBarnId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandling(fagsak)
        val behandlingBarn1 = behandlingBarn(behandlingId = behandling.id, personIdent = "1")
        val behandlingBarn2 = behandlingBarn(behandlingId = behandling.id, personIdent = "2")
        val behandlingBarn3 = behandlingBarn(behandlingId = behandling.id, personIdent = "3")
        val avtale1 = samværsavtale(behandlingId = behandling.id, behandlingBarnid = behandlingBarn1.id)
        val avtale2 = samværsavtale(behandlingId = behandling.id, behandlingBarnid = behandlingBarn2.id)
        val avtale3 = samværsavtale(behandlingId = behandling.id, behandlingBarnid = behandlingBarn3.id)

        behandlingRepository.insert(behandling)
        barnRepository.insert(behandlingBarn1)
        barnRepository.insert(behandlingBarn2)
        barnRepository.insert(behandlingBarn3)
        samværsavtaleRepository.insert(avtale1)
        samværsavtaleRepository.insert(avtale2)
        samværsavtaleRepository.insert(avtale3)

        samværsavtaleRepository.deleteByBehandlingIdAndBehandlingBarnId(behandling.id, behandlingBarn2.id)

        val lagredeAvtaler = samværsavtaleRepository.findByBehandlingId(behandling.id)

        assertThat(lagredeAvtaler.size).isEqualTo(2)
        assertThat(lagredeAvtaler.map { it.behandlingId }.toSet()).containsExactly(behandling.id)
        assertThat(lagredeAvtaler.map { it.behandlingBarnId }).containsExactlyInAnyOrder(behandlingBarn1.id, behandlingBarn3.id)
    }
}