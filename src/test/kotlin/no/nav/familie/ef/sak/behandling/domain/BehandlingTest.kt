package no.nav.familie.ef.sak.behandling.domain

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.HENLAGT
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.FEILREGISTRERT
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class BehandlingTest {

    val behandling = behandling(fagsak = fagsak())

    @Test
    internal fun `Skal kunne helegge FØRSTEGANGSBEHANDLING`() {
        val henlagtFeilregistrert = lagBehandlingMed(HENLAGT, FEILREGISTRERT, FØRSTEGANGSBEHANDLING)
        val henlagtTrukket = lagBehandlingMed(HENLAGT, TRUKKET_TILBAKE, FØRSTEGANGSBEHANDLING)
        assertThat(henlagtFeilregistrert.resultat).isEqualTo(HENLAGT)
        assertThat(henlagtTrukket.resultat).isEqualTo(HENLAGT)
        assertThat(henlagtFeilregistrert.henlagtÅrsak).isEqualTo(FEILREGISTRERT)
        assertThat(henlagtTrukket.henlagtÅrsak).isEqualTo(TRUKKET_TILBAKE)
    }

    @Test
    internal fun `Skal ikke kunne henlegge uten årsak `() {
        val feil: ApiFeil = assertThrows { lagBehandlingMed(HENLAGT, null, FØRSTEGANGSBEHANDLING) }
        assertThat(feil.feil).isEqualTo("Kan ikke henlegge behandling uten en årsak")
    }

    private fun lagBehandlingMed(
        behandlingResultat: BehandlingResultat,
        henlagtÅrsak: HenlagtÅrsak?,
        type: BehandlingType
    ): Behandling {
        return behandling.copy(resultat = behandlingResultat, henlagtÅrsak = henlagtÅrsak, type = type)
    }
}
