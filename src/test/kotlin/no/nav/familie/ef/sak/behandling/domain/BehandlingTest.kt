package no.nav.familie.ef.sak.behandling.domain

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.HENLAGT
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.BLANKETT
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.BEHANDLES_I_GOSYS
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.FEILREGISTRERT
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
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
    internal fun `Skal kunne helegge BLANKETT`() {
        val henlagt = lagBehandlingMed(HENLAGT, BEHANDLES_I_GOSYS, BLANKETT)
        assertThat(henlagt.resultat).isEqualTo(HENLAGT)
    }

    @Test
    internal fun `Skal ikke kunne helegge FØRSTEGANGSBEHANDLING med årsak forbeholdt BLANKETT`() {
        val feil: Feil = assertThrows { lagBehandlingMed(HENLAGT, BEHANDLES_I_GOSYS, FØRSTEGANGSBEHANDLING) }
        assertThat(feil.frontendFeilmelding).isEqualTo("Bare blankett kan henlegges med årsak BEHANDLES_I_GOSYS")
    }

    @Test
    internal fun `Skal ikke kunne helegge BLANKETT med årsak forbeholdt FØRSTEGANGSBEHANDLING`() {
        val feilRegistrert: Feil = assertThrows { lagBehandlingMed(HENLAGT, FEILREGISTRERT, BLANKETT) }
        val feilTrukket: Feil = assertThrows { lagBehandlingMed(HENLAGT, TRUKKET_TILBAKE, BLANKETT) }
        assertThat(feilRegistrert.frontendFeilmelding).isEqualTo("Blankett kan bare henlegges med årsak BEHANDLES_I_GOSYS")
        assertThat(feilTrukket.frontendFeilmelding).isEqualTo("Blankett kan bare henlegges med årsak BEHANDLES_I_GOSYS")
    }

    @Test
    internal fun `Skal ikke kunne henlegge uten årsak `() {
        val feil: Feil = assertThrows { lagBehandlingMed(HENLAGT, null, FØRSTEGANGSBEHANDLING) }
        assertThat(feil.frontendFeilmelding).isEqualTo("Kan ikke henlegge behandling uten en årsak")
    }

    private fun lagBehandlingMed(behandlingResultat: BehandlingResultat,
                                 henlagtÅrsak: HenlagtÅrsak?,
                                 type: BehandlingType): Behandling {
        return behandling.copy(resultat = behandlingResultat, henlagtÅrsak = henlagtÅrsak, type = type)
    }
}