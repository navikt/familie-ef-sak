package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattRevurdering
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class OpprettBehandlingUtilTest {

    private val fagsak = fagsak()

    @Nested
    inner class Førstegangsbehandling {
        @Test
        fun `mulig å lage behandling når det ikke finnes behandling fra før`() {
            validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf())
        }

        @Test
        fun `det skal være mulig å opprette hvis eksisterende behandling er henlagt førstegangsbehandling`() {
            val behandling = behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.HENLAGT,
                status = BehandlingStatus.FERDIGSTILT
            )
            validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(behandling))
        }

        @Test
        fun `det skal ikke være mulig å opprette hvis eksisterende behandling er en revurdering`() {
            val behandling = behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
                type = BehandlingType.REVURDERING
            )
            assertThatThrownBy {
                validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(behandling))
            }.hasMessage("Kan ikke opprette en førstegangsbehandling når forrige behandling ikke er en førstegangsbehandling")
        }

        @Test
        fun `det skal ikke være mulig å opprette hvis eksisterende behandling er avslått førstegangsbehandling`() {
            val behandling = behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.AVSLÅTT,
                status = BehandlingStatus.FERDIGSTILT
            )
            assertThatThrownBy {
                validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(behandling))
            }.hasMessage("Kan ikke opprette en førstegangsbehandling når siste behandling ikke er henlagt")
        }
    }

    @Nested
    inner class Revurdering {
        @Test
        fun `det skal ikke være mulig å opprette en revurdering hvis forrige behandling ikke er ferdigstilt`() {
            val behandlinger = listOf(
                behandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.FERDIGSTILT
                ),
                behandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.UTREDES
                ),
                behandling(
                    fagsak = fagsak,
                    status = BehandlingStatus.FERDIGSTILT
                )
            )
            assertThatThrownBy { validerKanOppretteNyBehandling(BehandlingType.REVURDERING, behandlinger) }
                .hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        fun `det skal være mulig å opprette en revurdering hvis eksisterende behandling er avslått førstegangsbehandling`() {
            val behandling = behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.AVSLÅTT,
                status = BehandlingStatus.FERDIGSTILT
            )
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf(behandling))
        }

        @Test
        fun `det skal ikke være mulig å opprette en revurdering om eksisterende behandling er henlagt`() {
            val behandling = behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.HENLAGT,
                status = BehandlingStatus.FERDIGSTILT
            )
            assertThatThrownBy {
                validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf(behandling))
            }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
        }

        @Test
        fun `skal ikke være mulig å opprette en revurdering hvis det ikke finnes en behandling fra før`() {
            assertThatThrownBy {
                validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf())
            }.hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
        }
    }

    @Nested
    inner class Migrering {

        @Test
        internal fun `skal kunne opprette en migrering uten tidligere behandlinger`() {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf(), erMigrering = true)
        }

        @Test
        internal fun `kan ikke opprette en migrering når tidligere behanding ikke er blankett`() {
            listOf(iverksattFørstegangsbehandling, iverksattRevurdering).forEach {
                assertThatThrownBy {
                    validerKanOppretteNyBehandling(
                        BehandlingType.REVURDERING,
                        listOf(it),
                        erMigrering = true
                    )
                }.hasMessage("Det er ikke mulig å opprette en migrering når det finnes en behandling fra før")
            }
        }
    }
}
