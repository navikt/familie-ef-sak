package no.nav.familie.ef.sak.no.nav.familie.ef.sak.metrics

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.metrics.domain.MålerRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MålerRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var målerRepository: MålerRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var fagsakRepository: FagsakRepository

    @BeforeEach
    fun init() {
        val fagsakFerdigstilt = fagsak()
        val behandlingFerdigstilt = behandling(fagsak = fagsakFerdigstilt,
                                               status = BehandlingStatus.FERDIGSTILT,
                                               resultat = BehandlingResultat.INNVILGET)
        val fagsakÅpen = fagsak()
        val behandlingÅpen = behandling(fagsak = fagsakÅpen, status = BehandlingStatus.UTREDES)
        fagsakRepository.insert(fagsakFerdigstilt)
        fagsakRepository.insert(fagsakÅpen)
        behandlingRepository.insert(behandlingFerdigstilt)
        behandlingRepository.insert(behandlingÅpen)
    }

    @Test
    fun `finnÅpneBehandlinger finner data for åpne behandlinger`() {
        val finnÅpneBehandlinger = målerRepository.finnÅpneBehandlinger()

        Assertions.assertThat(finnÅpneBehandlinger.size).isEqualTo(1)
    }

    @Test
    fun `finnKlarTilBehandling finner antall klar til behandling`() {
        val finnKlarTilBehandling = målerRepository.finnKlarTilBehandling()

        Assertions.assertThat(finnKlarTilBehandling.size).isEqualTo(1)
    }

    @Test
    fun `finnVedtak finner data om utførte vedtak`() {
        val finnVedtak = målerRepository.finnVedtak()

        Assertions.assertThat(finnVedtak.size).isEqualTo(1)
    }
}
