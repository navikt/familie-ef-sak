package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BehandlingStegTest {

    @Test
    fun `Tester rekkefølgen på steg`() {
        val riktigRekkefølge =
                listOf(StegType.VILKÅR,
                       StegType.BEREGNE_YTELSE,
                       StegType.SEND_TIL_BESLUTTER,
                       StegType.BESLUTTE_VEDTAK,
                       StegType.IVERKSETT_MOT_OPPDRAG,
                       StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                       StegType.JOURNALFØR_VEDTAKSBREV,
                       StegType.DISTRIBUER_VEDTAKSBREV,
                       StegType.FERDIGSTILLE_BEHANDLING,
                       StegType.BEHANDLING_FERDIGSTILT)

        validerSteg(riktigRekkefølge, BehandlingType.FØRSTEGANGSBEHANDLING)
    }

    @Test
    fun `Tester rekkefølgen på steg - TEKNISK_OPPHØR`() {
        val riktigRekkefølge = listOf(
                StegType.VILKÅR,
                StegType.BEREGNE_YTELSE,
                StegType.SEND_TIL_BESLUTTER,
                StegType.BESLUTTE_VEDTAK,
                StegType.IVERKSETT_MOT_OPPDRAG,
                StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.BEHANDLING_FERDIGSTILT)

        validerSteg(riktigRekkefølge, BehandlingType.TEKNISK_OPPHØR)
    }

    private fun validerSteg(riktigRekkefølge: List<StegType>, behandlingType: BehandlingType) {
        var steg: StegType = riktigRekkefølge.first()
        riktigRekkefølge.forEach {
            assertEquals(steg, it)
            steg = steg.hentNesteSteg(behandlingType = behandlingType)
        }
    }

    @Test
    fun testDisplayName() {
        assertEquals("Vilkår", StegType.VILKÅR.displayName())
    }

    @Test
    fun testErKompatibelMed() {
        assertTrue(StegType.VILKÅR.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertTrue(StegType.SEND_TIL_BESLUTTER.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertTrue(StegType.BESLUTTE_VEDTAK.erGyldigIKombinasjonMedStatus(BehandlingStatus.FATTER_VEDTAK))
        assertTrue(StegType.IVERKSETT_MOT_OPPDRAG.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.JOURNALFØR_VEDTAKSBREV.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.DISTRIBUER_VEDTAKSBREV.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.FERDIGSTILLE_BEHANDLING.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))

        assertFalse(StegType.VILKÅR.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertFalse(StegType.BEHANDLING_FERDIGSTILT.erGyldigIKombinasjonMedStatus(BehandlingStatus.OPPRETTET))
    }

}