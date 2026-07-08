package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.brev.MellomlagerBrevRepository
import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class MellomlagringBrevRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var mellomlagerBrevRepository: MellomlagerBrevRepository

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val mellomlagretBrev = MellomlagretBrev(behandling.id, "{}", "mal", "1", LocalDate.now())

        mellomlagerBrevRepository.insert(mellomlagretBrev)

        assertThat(mellomlagerBrevRepository.findById(behandling.id)).get().usingRecursiveComparison().isEqualTo(mellomlagretBrev)
    }

    @Test
    internal fun `upsert skal oppdatere eksisterende mellomlagret brev uten å feile på duplicate key`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val opprinneligDato = LocalDate.now().minusDays(1)

        mellomlagerBrevRepository.upsert(behandling.id, "{}", "mal", "1", opprinneligDato)
        mellomlagerBrevRepository.upsert(behandling.id, "{\"felt\":1}", "mal2", "2", LocalDate.now())

        val lagret = mellomlagerBrevRepository.findById(behandling.id).get()
        assertThat(lagret.brevverdier).isEqualTo("{\"felt\":1}")
        assertThat(lagret.brevmal).isEqualTo("mal2")
        assertThat(lagret.sanityVersjon).isEqualTo("2")
        assertThat(lagret.opprettetTid).isEqualTo(LocalDate.now())
    }
}
