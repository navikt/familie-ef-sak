package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VedtaksbrevRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var vedtaksbrevRepository: VedtaksbrevRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val vedtaksbrev = Vedtaksbrev(behandlingId = behandling.id,
                                      saksbehandlerBrevrequest = "testhallo",
                                      saksbehandlerHtml = null,
                                      brevmal = "brevmalnavn",
                                      saksbehandlersignatur = "Sakliga Behandlersen",
                                      besluttersignatur = "",
                                      beslutterPdf = null,
                                      enhet = "",
                                      saksbehandlerident = "",
                                      beslutterident = "")

        vedtaksbrevRepository.insert(vedtaksbrev)

        assertThat(vedtaksbrevRepository.findById(behandling.id).get()).isEqualTo(vedtaksbrev)
    }
}