package no.nav.familie.ef.sak.no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.brev.MellomlagerFritekstbrevRepository
import no.nav.familie.ef.sak.brev.domain.Fritekstbrev
import no.nav.familie.ef.sak.brev.domain.MellomlagretFritekstbrev
import no.nav.familie.ef.sak.brev.dto.FritekstBrevKategori
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class MellomlagerFritekstbrevRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var mellomlagerFritekstbrevRepository: MellomlagerFritekstbrevRepository

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val mellomlagretBrev = MellomlagretFritekstbrev(behandlingId = behandling.id,
                                                        brev = Fritekstbrev(overskrift = "Et testbrev", avsnitt = listOf(
                                                                FrittståendeBrevAvsnitt(deloverskrift = "En deloverskift",
                                                                                        innhold = "Noe innhold"))),
                                                        brevType = FritekstBrevKategori.VEDTAK_AVSLAG)

        mellomlagerFritekstbrevRepository.insert(mellomlagretBrev)

        Assertions.assertThat(mellomlagerFritekstbrevRepository.findById(behandling.id))
                .get().usingRecursiveComparison().isEqualTo(mellomlagretBrev)
    }
}