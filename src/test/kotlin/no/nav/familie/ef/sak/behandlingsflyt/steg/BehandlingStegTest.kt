package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BehandlingStegTest {

    @Test
    fun `Tester rekkefølgen på steg`() {
        val riktigRekkefølge =
            listOf(
                StegType.VILKÅR,
                StegType.BEREGNE_YTELSE,
                StegType.SEND_TIL_BESLUTTER,
                StegType.BESLUTTE_VEDTAK,
                StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT,
                StegType.LAG_SAKSBEHANDLINGSBLANKETT,
                StegType.FERDIGSTILLE_BEHANDLING,
                StegType.PUBLISER_VEDTAKSHENDELSE,
                StegType.BEHANDLING_FERDIGSTILT,
            )

        validerSteg(riktigRekkefølge)
    }

    private fun validerSteg(riktigRekkefølge: List<StegType>) {
        var steg: StegType = riktigRekkefølge.first()
        riktigRekkefølge.forEach {
            assertEquals(steg, it)
            steg = steg.hentNesteSteg()
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
        assertTrue(StegType.FERDIGSTILLE_BEHANDLING.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))

        assertFalse(StegType.VILKÅR.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertFalse(StegType.BEHANDLING_FERDIGSTILT.erGyldigIKombinasjonMedStatus(BehandlingStatus.OPPRETTET))
    }
}
