package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.BehandlingService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingService: BehandlingService

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering hvis forrige behandling ikke er ferdigstilt`() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak = fagsak,
                                               status = BehandlingStatus.UTREDES))
        assertThat(catchThrowable { behandlingService.opprettBehandling(BehandlingType.REVURDERING, fagsak.id) })
                .hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering hvis det ikke finnes en behandling fra før`() {
        val fagsak = fagsakRepository.insert(fagsak())
        assertThat(catchThrowable { behandlingService.opprettBehandling(BehandlingType.REVURDERING, fagsak.id) })
                .hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering hvis forrige behandling er blankett`() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak = fagsak,
                                               status = BehandlingStatus.FERDIGSTILT,
                                               type = BehandlingType.BLANKETT))
        assertThat(catchThrowable { behandlingService.opprettBehandling(BehandlingType.REVURDERING, fagsak.id) })
                .hasMessage("Siste behandling ble behandlet i infotrygd")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering hvis forrige behandling er teknisk opphør`() {
        val fagsak = fagsakRepository.insert(fagsak())
        behandlingRepository.insert(behandling(fagsak = fagsak,
                                               status = BehandlingStatus.FERDIGSTILT,
                                               type = BehandlingType.TEKNISK_OPPHØR))
        assertThat(catchThrowable { behandlingService.opprettBehandling(BehandlingType.REVURDERING, fagsak.id) })
                .hasMessage("Det er ikke mulig å lage en revurdering når siste behandlingen er teknisk opphør")
    }
}