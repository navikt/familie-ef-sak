package no.nav.familie.ef.sak.no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class BarnRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var barnRepository: BarnRepository

    @Test
    internal fun `skal lagre ned barn`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val nyttBarn = BehandlingBarn(id = UUID.randomUUID(),
                                      behandlingId = behandling.id,
                                      søknadBarnId = UUID.randomUUID(),
                                      personIdent = "12345678901",
                                      navn = "Test Barnesen",
                                      fødselTermindato = null)
        barnRepository.insert(nyttBarn)
        val barnet = barnRepository.findByIdOrThrow(nyttBarn.id)
        assertThat(barnet).isEqualTo(nyttBarn)

        val barnForBehandling = barnRepository.findByBehandlingId(nyttBarn.behandlingId)
        assertThat(barnForBehandling).hasSize(1)
        assertThat(barnForBehandling.first()).isEqualTo(nyttBarn)

    }
}