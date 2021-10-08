package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.blankett.BlankettRepository
import no.nav.familie.ef.sak.blankett.VedtaBlankettSteg
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.Henlegge
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtaBlankettStegTest {

    private val vedtakService: VedtakService = mockk()
    private val blankettRepository: BlankettRepository = mockk()
    private val vedtaBlankettSteg = VedtaBlankettSteg(vedtakService, blankettRepository)

    @Test
    internal fun `skal opprette nytt vedtak - innvilget`() {
        val behandling = behandling(fagsak(),
                                    steg = StegType.VILKÅR,
                                    status = BehandlingStatus.UTREDES,
                                    type = BehandlingType.BLANKETT)
        val request = Innvilget(
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

        every {
            blankettRepository.deleteById(any())
        } just Runs

        vedtaBlankettSteg.utførOgReturnerNesteSteg(behandling, request)

    }


    @Test
    internal fun `skal feile hvis nytt vedtak er førstegangsbehandling`() {
        val behandling = behandling(fagsak(),
                                    steg = StegType.VILKÅR,
                                    status = BehandlingStatus.UTREDES,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING)
        val request = Innvilget(
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

        every {
            blankettRepository.deleteById(any())
        } just Runs

        assertThrows<IllegalStateException> { vedtaBlankettSteg.utførOgReturnerNesteSteg(behandling, request) }

    }

    @Test
    internal fun `skal feile hvis nytt vedtak er henlagt - ikke implementert ennå`() {
        val behandling = behandling(fagsak(),
                                    steg = StegType.VILKÅR,
                                    status = BehandlingStatus.UTREDES,
                                    type = BehandlingType.BLANKETT)
        val request = Henlegge(
                resultatType = ResultatType.HENLEGGE
        )

        assertThrows<Feil> { vedtaBlankettSteg.utførOgReturnerNesteSteg(behandling, request) }

    }

    @Test
    internal fun `skal forsøke å slette blankett ved lagring av vedtak`(){
        val behandling = behandling(fagsak(),
                                    steg = StegType.VILKÅR,
                                    status = BehandlingStatus.UTREDES,
                                    type = BehandlingType.BLANKETT)
        val request = Innvilget(
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

        every {
            blankettRepository.deleteById(any())
        } just Runs

        vedtaBlankettSteg.utførOgReturnerNesteSteg(behandling, request)

        verify { blankettRepository.deleteById(behandling.id) }


    }
}