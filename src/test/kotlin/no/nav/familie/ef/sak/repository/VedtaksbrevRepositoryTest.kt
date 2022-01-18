package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VedtaksbrevRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var vedtaksbrevRepository: VedtaksbrevRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val vedtaksbrev = Vedtaksbrev(behandlingId = behandling.id,
                                      saksbehandlerBrevrequest = "testhallo",
                                      brevmal = "brevmalnavn",
                                      saksbehandlersignatur = "Sakliga Behandlersen",
                                      besluttersignatur = "",
                                      beslutterPdf = null, "", "", "")

        vedtaksbrevRepository.insert(vedtaksbrev)

        assertThat(vedtaksbrevRepository.findById(behandling.id).get()).isEqualTo(vedtaksbrev)
    }
}