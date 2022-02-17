package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.behandling.OpprettBehandlingUtil.validerKanOppretteNyBehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattFørstegangsbehandling
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil.iverksattRevurdering
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class OpprettBehandlingUtilTest {

    private val fagsak = fagsak()

    @Test
    fun `førstegangsbehandling - mulig å lage behandling når det ikke finnes behandling fra før`() {
        assertThat(catchThrowable { validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, listOf(), null) })
                .doesNotThrowAnyException()
    }

    @Test
    fun `førstegangsbehandling - forrige behandling må være blankett eller teknisk opphør`() {
        BehandlingType.values().forEach {
            val tidligereBehandlinger = listOf(behandling(fagsakMedPerson = fagsak,
                                                          type = it,
                                                          status = BehandlingStatus.FERDIGSTILT))
            if (it == BehandlingType.TEKNISK_OPPHØR || it == BehandlingType.BLANKETT) {
                validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, tidligereBehandlinger, null)
            } else {
                assertThat(catchThrowable {
                    validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, tidligereBehandlinger, null)
                }).hasMessage("Siste behandlingen for en førstegangsbehandling må være av typen blankett eller teknisk opphør")
            }
        }
    }

    @Test
    fun `førstegangsbehandling - det skal ikke være mulig å opprette hvis eksisterende behandling er avslått førstegangsbehandling`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.FØRSTEGANGSBEHANDLING,
                                           listOf(behandling(fagsakMedPerson = fagsak,
                                                             resultat = BehandlingResultat.AVSLÅTT,
                                                             status = BehandlingStatus.FERDIGSTILT)
                                           ), null)
        }).hasMessage("Siste behandlingen for en førstegangsbehandling må være av typen blankett eller teknisk opphør")
    }


    @Test
    fun `revurdering - det skal ikke være mulig å opprette en revurdering hvis forrige behandling ikke er ferdigstilt`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsakMedPerson = fagsak,
                                                             status = BehandlingStatus.FERDIGSTILT),
                                                  behandling(fagsakMedPerson = fagsak,
                                                             status = BehandlingStatus.UTREDES),
                                                  behandling(fagsakMedPerson = fagsak,
                                                             status = BehandlingStatus.FERDIGSTILT)), null)
        }).hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
    }

    @Test
    fun `revurdering - det skal være mulig å opprette en revurdering hvis eksisterende behandling er avslått førstegangsbehandling`() {
        validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                       listOf(behandling(fagsakMedPerson = fagsak,
                                                         resultat = BehandlingResultat.AVSLÅTT,
                                                         status = BehandlingStatus.FERDIGSTILT)
                                       ), null)
    }

    @Test
    fun `revurdering - det skal ikke være mulig å opprette en revurdering om eksisterende behandling er henlagt`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsakMedPerson = fagsak,
                                                             resultat = BehandlingResultat.HENLAGT,
                                                             status = BehandlingStatus.FERDIGSTILT)
                                           ), null)
        }).hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
    }

    @Test
    fun `revurdering - skal ikke være mulig å opprette en revurdering hvis det ikke finnes en behandling fra før`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf(), null)
        }).hasMessage("Det finnes ikke en tidligere behandling på fagsaken")
    }

    @Test
    fun `revurdering - skal ikke være mulig å opprette en revurdering hvis forrige behandling er blankett`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsakMedPerson = fagsak,
                                                             type = BehandlingType.BLANKETT,
                                                             henlagtÅrsak = HenlagtÅrsak.BEHANDLES_I_GOSYS,
                                                             status = BehandlingStatus.FERDIGSTILT)), null)
        }).hasMessage("Siste behandling ble behandlet i infotrygd, denne må migreres")
    }

    @Test
    fun `revurdering - skal ikke være mulig å opprette en revurdering hvis forrige behandling er teknisk opphør`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(behandling(fagsakMedPerson = fagsak,
                                                             type = BehandlingType.TEKNISK_OPPHØR,
                                                             status = BehandlingStatus.FERDIGSTILT)), null)
        }).hasMessage("Det er ikke mulig å lage en revurdering når siste behandlingen er teknisk opphør")
    }

    @Test
    fun `teknisk opphør - siste behandlingen må være iverksatt`() {
        validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR, listOf(iverksattRevurdering), iverksattRevurdering)
        validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR,
                                       listOf(iverksattFørstegangsbehandling),
                                       iverksattFørstegangsbehandling)
    }

    @Test
    fun `teknisk opphør - siste behandlingen kan ikke være teknisk opphør`() {
        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR,
                                           listOf(BehandlingOppsettUtil.iverksattTekniskOpphør),
                                           BehandlingOppsettUtil.iverksattTekniskOpphør)
        }).hasMessage("Kan ikke opphøre en allerede opphørt behandling")
    }

    @Test
    fun `teknisk opphør - skal kaste feil hvis siste behandling ikke er iverksatt`() {
        assertThat(catchThrowable { validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR, listOf(), null) })
                .hasMessage("Det finnes ikke en tidligere behandling for fagsaken")

        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR,
                                           listOf(BehandlingOppsettUtil.ferdigstiltBlankett), null)
        }).hasMessage("Siste behandlingen må være iverksatt for å kunne utføre teknisk opphør")

        assertThat(catchThrowable {
            validerKanOppretteNyBehandling(BehandlingType.TEKNISK_OPPHØR,
                                           listOf(BehandlingOppsettUtil.henlagtRevurdering), null)
        }).hasMessage("Det finnes ikke en tidligere behandling for fagsaken")
    }

    @Nested
    inner class Migrering {

        @Test
        internal fun `skal kunne opprette en migrering uten tidligere behandlinger`() {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING, listOf(), null, erMigrering = true)
        }

        @Test
        internal fun `skal kunne opprette en migrering når tidligere behanding er blankett`() {
            validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                           listOf(BehandlingOppsettUtil.ferdigstiltBlankett),
                                           null,
                                           erMigrering = true)
        }

        @Test
        internal fun `kan ikke opprette en migrering når tidligere behanding ikke er blankett`() {
            listOf(iverksattFørstegangsbehandling, iverksattRevurdering).forEach {
                assertThat(catchThrowable {
                    validerKanOppretteNyBehandling(BehandlingType.REVURDERING,
                                                   listOf(it), null,
                                                   erMigrering = true)
                }).hasMessage("Det er ikke mulig å opprette en migrering når det finnes en behandling fra før")
            }
        }
    }

}
