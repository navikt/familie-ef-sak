package no.nav.familie.ef.sak.util

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.util.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test

internal class OpprettBehandlingUtilTest {

    private val fagsak = fagsak()

    @Test
    internal fun `det skal ikke være mulig å opprette en revurdering hvis forrige behandling ikke er ferdigstilt`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsak = fagsak,
                                                             status = BehandlingStatus.FERDIGSTILT),
                                                  behandling(fagsak = fagsak,
                                                             status = BehandlingStatus.UTREDES),
                                                  behandling(fagsak = fagsak,
                                                             status = BehandlingStatus.FERDIGSTILT)))
        }).hasMessage("Kan ikke å opprette blankettbehandling når fagsaken allerede har andre typer behandlinger")
    }

    @Test
    internal fun `førstegangsbehandling - forrige behandling må være blankett eller teknisk opphør`() {
        BehandlingType.values().forEach {
            val tidligereBehandlinger = listOf(behandling(fagsak = fagsak,
                                                          type = it,
                                                          status = BehandlingStatus.FERDIGSTILT))
            if (it == BehandlingType.TEKNISK_OPPHØR || it == BehandlingType.BLANKETT) {
                validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, tidligereBehandlinger)
            } else {
                assertThat(catchThrowable {
                    validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, tidligereBehandlinger)
                }).hasMessage("Siste behandlingen for en førstegangsbehandling må være av typen blankett eller teknisk opphør")
            }
        }
    }

    @Test
    internal fun `revurdering - skal ikke være mulig å opprette en revurdering hvis det ikke finnes en behandling fra før`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf())
        }).hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
    }

    @Test
    internal fun `revurdering - skal ikke være mulig å opprette en revurdering hvis forrige behandling er blankett`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsak = fagsak,
                                                             type = BehandlingType.BLANKETT,
                                                             status = BehandlingStatus.FERDIGSTILT)))
        }).hasMessage("Siste behandling ble behandlet i infotrygd")
    }

    @Test
    internal fun `revurdering - skal ikke være mulig å opprette en revurdering hvis forrige behandling er teknisk opphør`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsak = fagsak,
                                                             type = BehandlingType.TEKNISK_OPPHØR,
                                                             status = BehandlingStatus.FERDIGSTILT)))
        }).hasMessage("Det er ikke mulig å lage en revurdering når siste behandlingen er teknisk opphør")
    }

}