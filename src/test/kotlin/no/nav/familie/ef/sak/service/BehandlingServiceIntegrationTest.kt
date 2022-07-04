package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class BehandlingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository
    @Autowired
    lateinit var behandlingService: BehandlingService
    private val behandlingÅrsak = BehandlingÅrsak.SØKNAD

    @Test
    internal fun `opprettBehandling skal ikke være mulig å opprette en revurdering om forrige behandling ikke er ferdigstilt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                status = BehandlingStatus.UTREDES
            )
        )
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak
            )
        }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    internal fun `opprettBehandling - skal ikke være mulig å opprette en revurdering om det ikke finnes en behandling fra før`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        assertThatThrownBy {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak
            )
        }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
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
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val behandling2 = behandlingRepository.insert(behandling(fagsak))

        assertThat(behandlingService.hentBehandlinger(setOf(behandling.id, behandling2.id))).hasSize(2)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis kun avslått`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(
            behandling(fagsak).copy(
                resultat = BehandlingResultat.AVSLÅTT,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(behandling.id)
    }

    @Test
    internal fun `skal finne siste behandling med avslåtte hvis avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val avslag = behandlingRepository.insert(
            behandling(fagsak).copy(
                resultat = BehandlingResultat.AVSLÅTT,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        behandlingRepository.insert(
            behandling(fagsak).copy(
                resultat = BehandlingResultat.HENLAGT,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(avslag.id)
    }

    @Test
    internal fun `skal plukke ut førstegangsbehandling hvis det finnes førstegangsbehandling, avslått og henlagt`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val førstegang = behandlingRepository.insert(
            behandling(fagsak).copy(
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        behandlingRepository.insert(
            behandling(fagsak).copy(
                resultat = BehandlingResultat.AVSLÅTT,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        behandlingRepository.insert(
            behandling(fagsak).copy(
                resultat = BehandlingResultat.HENLAGT,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)
        assertThat(sisteBehandling?.id).isEqualTo(førstegang.id)
    }
}
