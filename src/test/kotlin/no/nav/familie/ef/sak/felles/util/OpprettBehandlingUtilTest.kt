package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattRevurdering
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.lagBehandlingerForSisteIverksatte
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.behandling.OpprettBehandlingUtil.sistIverksatteBehandling
import no.nav.familie.ef.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class OpprettBehandlingUtilTest {

    private val fagsak = fagsak()

    @Test
    internal fun `førstegangsbehandling - mulig å lage behandling når det ikke finnes behandling fra før`() {
        assertThat(catchThrowable { validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf()) })
                .doesNotThrowAnyException()
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
    internal fun `revurdering - det skal ikke være mulig å opprette en revurdering hvis forrige behandling ikke er ferdigstilt`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsak = fagsak,
                                                             status = BehandlingStatus.FERDIGSTILT),
                                                  behandling(fagsak = fagsak,
                                                             status = BehandlingStatus.UTREDES),
                                                  behandling(fagsak = fagsak,
                                                             status = BehandlingStatus.FERDIGSTILT)))
        }).hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
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

    @Test
    internal fun `teknisk opphør - siste behandlingen må være iverksatt`() {
        validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR, listOf(iverksattRevurdering))
        validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR, listOf(iverksattFørstegangsbehandling))
    }

    @Test
    internal fun `teknisk opphør - siste behandlingen kan ikke være teknisk opphør`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR,
                                           listOf(BehandlingOppsettUtil.iverksattTekniskOpphør))
        }).hasMessage("Kan ikke opphøre en allerede opphørt behandling")
    }

    @Test
    internal fun `teknisk opphør - skal kaste feil hvis siste behandling ikke er iverksatt`() {
        assertThat(catchThrowable { validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR, listOf()) })
                .hasMessage("Det finnes ikke en tidligere behandling for fagsaken")

        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR,
                                           listOf(BehandlingOppsettUtil.ferdigstiltBlankett))
        }).hasMessage("Siste behandlingen må være iverksatt for å kunne utføre teknisk opphør")

        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR,
                                           listOf(BehandlingOppsettUtil.annullertRevurdering))
        }).hasMessage("Det finnes ikke en tidligere behandling for fagsaken")
    }

    @Test
    internal fun `finnSisteIverksatteBehandling skal finne id til siste behandling som er ferdigstilt, ikke annulert eller blankett`() {
        val behandlinger = lagBehandlingerForSisteIverksatte()
        val førstegangsbehandling = iverksattFørstegangsbehandling
        val sistIverksatteBehandlingId = sistIverksatteBehandling(behandlinger)?.id

        assertThat(sistIverksatteBehandlingId).isNotNull
        assertThat(sistIverksatteBehandlingId).isEqualTo(førstegangsbehandling.id)
    }

    @Test
    internal fun `skal returnere tidligere behandling hvis den er iverksatt`() {
        assertThat(sistIverksatteBehandling(listOf(iverksattFørstegangsbehandling))).isNotNull
        assertThat(sistIverksatteBehandling(listOf(iverksattRevurdering))).isNotNull
    }

    @Test
    internal fun `skal returnere sist iverksatte behandlingen`() {
        fun iverksatt(tid: LocalDateTime) = iverksattRevurdering.copy(id = UUID.randomUUID(),
                                                                      sporbar = Sporbar(opprettetTid = tid))

        val behandlingA = iverksatt(LocalDateTime.now().minusDays(5))
        val behandlingB = iverksatt(LocalDateTime.now())
        val behandlingC = iverksatt(LocalDateTime.now().plusDays(5))

        val behandlingerMedSistBehandlerIMidten = listOf(behandlingA, behandlingC, behandlingB)
        assertThat(sistIverksatteBehandling(behandlingerMedSistBehandlerIMidten)?.id).isEqualTo(behandlingC.id)
    }

    @Test
    internal fun `skal ikke returnere tidligere behandling for førstegangsbehandling som ikke er iverksatt`() {
        assertThat(sistIverksatteBehandling(listOf(BehandlingOppsettUtil.førstegangsbehandlingUnderBehandling))).isNull()
        assertThat(sistIverksatteBehandling(listOf(BehandlingOppsettUtil.annullertFørstegangsbehandling))).isNull()
        assertThat(sistIverksatteBehandling(listOf(BehandlingOppsettUtil.ferdigstiltBlankett))).isNull()
        assertThat(sistIverksatteBehandling(listOf(BehandlingOppsettUtil.annullertRevurdering))).isNull()
        assertThat(sistIverksatteBehandling(listOf(BehandlingOppsettUtil.revurderingUnderArbeid))).isNull()
    }
}