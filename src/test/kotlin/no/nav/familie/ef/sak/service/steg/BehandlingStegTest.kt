package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BehandlingStegTest {

    @Test
    fun `Tester rekkefølgen på steg`() {
        val riktigRekkefølge =
                listOf(StegType.REGISTRERE_OPPLYSNINGER,
                       StegType.VILKÅRSVURDERE_INNGANGSVILKÅR,
                       StegType.VILKÅRSVURDERE_STØNAD,
                       StegType.BEREGNE_YTELSE,
                       StegType.SEND_TIL_BESLUTTER,
                       StegType.BESLUTTE_VEDTAK,
                       StegType.IVERKSETT_MOT_OPPDRAG,
                       StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI,
                       StegType.JOURNALFØR_VEDTAKSBREV,
                       StegType.DISTRIBUER_VEDTAKSBREV,
                       StegType.FERDIGSTILLE_BEHANDLING,
                       StegType.BEHANDLING_FERDIGSTILT)

        var steg = StegType.REGISTRERE_OPPLYSNINGER
        riktigRekkefølge.forEach {
            assertEquals(steg, it)
            steg = it.hentNesteSteg(utførendeStegType = steg)
        }

    }

    @Test
    fun testDisplayName() {
        assertEquals("Vilkårsvurdere stønad", StegType.VILKÅRSVURDERE_STØNAD.displayName())
    }

    @Test
    fun testErKompatibelMed() {
        assertTrue(StegType.REGISTRERE_OPPLYSNINGER.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertTrue(StegType.VILKÅRSVURDERE_INNGANGSVILKÅR.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertTrue(StegType.VILKÅRSVURDERE_STØNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertTrue(StegType.SEND_TIL_BESLUTTER.erGyldigIKombinasjonMedStatus(BehandlingStatus.UTREDES))
        assertTrue(StegType.BESLUTTE_VEDTAK.erGyldigIKombinasjonMedStatus(BehandlingStatus.FATTER_VEDTAK))
        assertTrue(StegType.IVERKSETT_MOT_OPPDRAG.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.JOURNALFØR_VEDTAKSBREV.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.DISTRIBUER_VEDTAKSBREV.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertTrue(StegType.FERDIGSTILLE_BEHANDLING.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))

        assertFalse(StegType.VILKÅRSVURDERE_STØNAD.erGyldigIKombinasjonMedStatus(BehandlingStatus.IVERKSETTER_VEDTAK))
        assertFalse(StegType.BEHANDLING_FERDIGSTILT.erGyldigIKombinasjonMedStatus(BehandlingStatus.OPPRETTET))
    }

}