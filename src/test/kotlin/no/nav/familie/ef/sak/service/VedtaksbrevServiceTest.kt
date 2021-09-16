package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakPerson
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.steg.StegType
import no.nav.familie.ef.sak.vedtak.BrevClient
import no.nav.familie.ef.sak.vedtak.Vedtaksbrev
import no.nav.familie.ef.sak.vedtak.VedtaksbrevRepository
import no.nav.familie.ef.sak.vedtak.VedtaksbrevService
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtaksbrevServiceTest {


    private val fagsak = fagsak(setOf(FagsakPerson("")))
    private val behandling = behandling(fagsak)

    private val brevClient = mockk<BrevClient>()
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val behandlingService = mockk<BehandlingService>()

    private val vedtaksbrevService = VedtaksbrevService(brevClient, vedtaksbrevRepository, behandlingService)

    private val vedtaksbrev = Vedtaksbrev(behandling.id,
                                          "123",
                                          "malnavn",
                                          "Saksbehandler Signatur",
                                          null,
                                          null)


    @Test
    internal fun `skal legge på signatur og lage pdf ved lagBeslutterBrev`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling()
        val vedtaksbrevSlot = slot<Vedtaksbrev>()

        val saksbehandlerNavn = "Saksbehandler Saksbehandlersen"
        mockBrukerContext(saksbehandlerNavn)
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { brevClient.genererBrev(any()) } returns "123".toByteArray()
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } returns vedtaksbrev

        vedtaksbrevService.lagBeslutterBrev(behandling.id)
        assertThat(vedtaksbrevSlot.captured.besluttersignatur).isEqualTo(saksbehandlerNavn)
        assertThat(vedtaksbrevSlot.captured).usingRecursiveComparison()
                .ignoringFields("besluttersignatur", "beslutterPdf")
                .isEqualTo(vedtaksbrev)

        clearBrukerContext()
    }

    @Test
    internal fun `skal feile når signatur ikke finnes`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling()
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { vedtaksbrevRepository.update(any()) } returns vedtaksbrev

        assertThrows<IllegalStateException> { vedtaksbrevService.lagBeslutterBrev(behandling.id) }
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig steg`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling().copy(steg = StegType.VILKÅR)
        assertThrows<Feil> { vedtaksbrevService.lagBeslutterBrev(behandling.id) }
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig status`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling().copy(status = BehandlingStatus.FERDIGSTILT)
        assertThrows<Feil> { vedtaksbrevService.lagBeslutterBrev(behandling.id) }

        every { behandlingService.hentBehandling(any()) } returns lagBehandling().copy(status = BehandlingStatus.UTREDES)
        assertThrows<Feil> { vedtaksbrevService.lagBeslutterBrev(behandling.id) }
    }

    @Test
    internal fun `lagSaksbehandlerBrev skal kaste feil når behandling er låst for videre behandling`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling().copy(status = BehandlingStatus.FERDIGSTILT)
        assertThrows<Feil> { vedtaksbrevService.lagSaksbehandlerBrev(behandling.id, TextNode(""), "") }
    }

    private fun lagBehandling() = behandling(fagsak(),
                                             status = BehandlingStatus.FATTER_VEDTAK,
                                             steg = StegType.BESLUTTE_VEDTAK)

    @Test
    internal fun `JsonNode toString fungerer som forventet`() {
        val json = """{"name":"John"}"""
        assertThat(objectMapper.readTree(json).toString()).isEqualTo(json)
    }

}