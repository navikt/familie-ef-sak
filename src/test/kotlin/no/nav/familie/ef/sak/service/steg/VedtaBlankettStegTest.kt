package no.nav.familie.ef.sak.service.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtaBlankettStegTest {

    private val vedtakService: VedtakService = mockk()
    private val vedtaBlankettSteg = VedtaBlankettSteg(vedtakService)

    @Test
    internal fun `skal opprette nytt vedtak - innvilget`() {
        val behandling = behandling(fagsak(),
                                    steg = StegType.VILKÅRSVURDERE_INNGANGSVILKÅR,
                                    status = BehandlingStatus.UTREDES,
                                    type = BehandlingType.BLANKETT)
        val request = VedtakDto(
                resultatType = ResultatType.INNVILGE,
                "En periodebegrunnelse",
                "En inntektBegrunnelse",
                emptyList(),
                emptyList()

        )

        every {
            vedtakService.lagreVedtak(any(), any())
        } returns behandling.id

        every {
            vedtakService.slettVedtakHvisFinnes(any())
        } just Runs

        vedtaBlankettSteg.utførOgReturnerNesteSteg(behandling, request)

    }


    @Test
    internal fun `skal feile hvis nytt vedtak er førstegangsbehandling`() {
        val behandling = behandling(fagsak(),
                                    steg = StegType.VILKÅRSVURDERE_INNGANGSVILKÅR,
                                    status = BehandlingStatus.UTREDES,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING)
        val request = VedtakDto(
                resultatType = ResultatType.INNVILGE,
                "En periodebegrunnelse",
                "En inntektBegrunnelse",
                emptyList(),
                emptyList()

        )

        every {
            vedtakService.lagreVedtak(any(), any())
        } returns behandling.id

        every {
            vedtakService.slettVedtakHvisFinnes(any())
        } just Runs

        assertThrows<IllegalStateException> { vedtaBlankettSteg.utførOgReturnerNesteSteg(behandling, request) }

    }

    @Test
    internal fun `skal feile hvis nytt vedtak er henlagt - ikke implementert ennå`() {
        val behandling = behandling(fagsak(),
                                    steg = StegType.VILKÅRSVURDERE_INNGANGSVILKÅR,
                                    status = BehandlingStatus.UTREDES,
                                    type = BehandlingType.BLANKETT)
        val request = VedtakDto(
                resultatType = ResultatType.HENLEGGE,
                "En periodebegrunnelse",
                "En inntektBegrunnelse",
                emptyList(),
                emptyList()

        )

        assertThrows<Feil> { vedtaBlankettSteg.utførOgReturnerNesteSteg(behandling, request) }

    }
}