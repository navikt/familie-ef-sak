package no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.service.VurderingService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class InngangsvilkårStegTest {

    private val vurderingService = mockk<VurderingService>()
    private val inngangsvilkårSteg = InngangsvilkårSteg(vurderingService)

    @Test
    fun `skal feile validering når inngangsvilkår ikke er vurdert`() {
        val behandling = behandling(fagsak())
        every { vurderingService.hentInngangsvilkårSomManglerVurdering(behandling.id) } returns
                listOf(VilkårType.FORUTGÅENDE_MEDLEMSKAP)

        val exception = assertThrows<Feil> { inngangsvilkårSteg.postValiderSteg(behandling) }
        assertEquals("Følgende inngangsvilkår mangler vurdering: \n${VilkårType.FORUTGÅENDE_MEDLEMSKAP.beskrivelse}",
                     exception.frontendFeilmelding)
    }
}