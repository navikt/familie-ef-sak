package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class BehandlingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var behandlingService: BehandlingService
    private val behandlingÅrsak = BehandlingÅrsak.SØKNAD

    @Test
    internal fun `opprettBehandling skal ikke være mulig å opprette en revurdering om forrige behandling ikke er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        behandlingRepository.insert(behandling(fagsak = fagsak,
                                               status = BehandlingStatus.UTREDES))
        assertThatThrownBy {
            behandlingService.opprettBehandling(BehandlingType.REVURDERING,
                                                fagsak.id,
                                                behandlingsårsak = behandlingÅrsak)
        }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering om det ikke finnes en behandling fra før`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThatThrownBy {
            behandlingService.opprettBehandling(BehandlingType.REVURDERING,
                                                fagsak.id,
                                                behandlingsårsak = behandlingÅrsak)
        }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering hvis forrige behandling er blankett`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        behandlingRepository.insert(behandling(fagsak = fagsak,
                                               status = BehandlingStatus.FERDIGSTILT,
                                               type = BehandlingType.BLANKETT))
        assertThatThrownBy {
            behandlingService.opprettBehandling(BehandlingType.REVURDERING,
                                                fagsak.id,
                                                behandlingsårsak = behandlingÅrsak)
        }.hasMessage("Siste behandling ble behandlet i infotrygd, denne må migreres")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering om forrige behandling er teknisk opphør`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        behandlingRepository.insert(behandling(fagsak = fagsak,
                                               status = BehandlingStatus.FERDIGSTILT,
                                               type = BehandlingType.TEKNISK_OPPHØR))
        assertThat(catchThrowable {
            behandlingService.opprettBehandling(BehandlingType.REVURDERING,
                                                fagsak.id,
                                                behandlingsårsak = behandlingÅrsak)
        })
                .hasMessage("Det er ikke mulig å lage en revurdering når siste behandlingen er teknisk opphør")
    }

    @Test
    internal fun `hentBehandlinger - skal kaste feil hvis behandling ikke finnes`() {
        assertThatThrownBy { behandlingService.hentBehandlinger(setOf(UUID.randomUUID())) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Finner ikke Behandling for")
    }

    @Test
    internal fun `hentBehandlinger - skal returnere behandlinger`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingService.hentBehandlinger(setOf(behandling.id, behandling2.id))).hasSize(2)
    }
}