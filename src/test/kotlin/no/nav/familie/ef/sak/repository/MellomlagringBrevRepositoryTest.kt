package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.MellomlagerBrevRepository
import no.nav.familie.ef.sak.repository.domain.MellomlagretBrev
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class MellomlagringBrevRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var mellomlagerBrevRepository: MellomlagerBrevRepository

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val mellomlagretBrev = MellomlagretBrev(behandling.id, "{}", "mal", "1")

        mellomlagerBrevRepository.insert(mellomlagretBrev)

        assertThat(mellomlagerBrevRepository.findById(behandling.id)).get().usingRecursiveComparison().isEqualTo(mellomlagretBrev)
    }
}