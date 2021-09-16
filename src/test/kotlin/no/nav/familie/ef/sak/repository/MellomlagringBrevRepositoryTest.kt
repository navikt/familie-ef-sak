package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.vedtak.MellomlagerBrevRepository
import no.nav.familie.ef.sak.vedtak.domain.MellomlagretBrev
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class MellomlagringBrevRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var mellomlagerBrevRepository: MellomlagerBrevRepository

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val mellomlagretBrev = MellomlagretBrev(behandling.id, "{}", "mal", "1", LocalDate.now())

        mellomlagerBrevRepository.insert(mellomlagretBrev)

        assertThat(mellomlagerBrevRepository.findById(behandling.id)).get().usingRecursiveComparison().isEqualTo(mellomlagretBrev)
    }
}