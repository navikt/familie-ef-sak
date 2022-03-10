package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtaksbrevServiceTest {

    private val fagsak = fagsak(setOf(PersonIdent("12345678910")))
    private val behandling = behandling(fagsak)

    private val brevClient = mockk<BrevClient>()
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val brevsignaturService = mockk<BrevsignaturService>()

    private val vedtaksbrevService =
            VedtaksbrevService(brevClient,
                               vedtaksbrevRepository,
                               personopplysningerService,
                               brevsignaturService)

    private val vedtaksbrev: Vedtaksbrev = lagVedtaksbrev("malnavn")

    private val fritekstBrevDto = lagVedtaksbrevFritekstDto()

    @Test
    internal fun `skal legge på signatur og lage pdf ved lagBeslutterBrev`() {
        val vedtaksbrevSlot = slot<Vedtaksbrev>()

        val beslutterNavn = "456"
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { brevClient.genererBrev(any()) } returns "enPdf".toByteArray()
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } returns vedtaksbrev
        val signaturDto = SignaturDto(beslutterNavn, "enhet", false)
        every { brevsignaturService.lagSignaturMedEnhet(any<Saksbehandling>()) } returns signaturDto

        mockBrukerContext(beslutterNavn)
        vedtaksbrevService.lagBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))
        clearBrukerContext()

        assertThat(vedtaksbrevSlot.captured.besluttersignatur).isEqualTo(beslutterNavn)
        assertThat(vedtaksbrevSlot.captured).usingRecursiveComparison()
                .ignoringFields("besluttersignatur", "beslutterPdf", "beslutterident", "enhet")
                .isEqualTo(vedtaksbrev)
    }

    @Test
    internal fun `skal legge på signatur og lage pdf ved lagSaksbehandlerFritekstbrev`() {
        val vedtaksbrevSlot = slot<Vedtaksbrev>()

        val saksbehandlerNavn = "Saksbehandler Saksbehandlersen"
        val ident = "12345678910"
        val gjeldendeNavn = "Navn Navnesen"
        val navnMap = mapOf(ident to gjeldendeNavn)

        mockBrukerContext(saksbehandlerNavn)
        every { personopplysningerService.hentGjeldeneNavn(any()) } returns navnMap
        every { vedtaksbrevRepository.existsById(any()) } returns true
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } returns vedtaksbrev
        every { brevClient.genererBrev(any()) } returns "123".toByteArray()
        val signaturDto = SignaturDto(saksbehandlerNavn, "enhet", false)
        every { brevsignaturService.lagSignaturMedEnhet(any<Saksbehandling>()) } returns signaturDto

        vedtaksbrevService.lagSaksbehandlerFritekstbrev(fritekstBrevDto, saksbehandling(fagsak, behandlingForSaksbehandler))
        assertThat(vedtaksbrevSlot.captured.saksbehandlersignatur).isEqualTo(saksbehandlerNavn)

        clearBrukerContext()
    }

    @Test
    internal fun `lagFritekstSaksbehandlerBrev skal kaste feil når behandling er låst for videre behandling`() {
        val feil = assertThrows<Feil> {
            vedtaksbrevService.lagSaksbehandlerFritekstbrev(fritekstBrevDto,
                                                            saksbehandling(fagsak, behandlingForBeslutter))
        }
        assertThat(feil.message).contains("Behandling er i feil steg")
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig steg`() {
        assertThrows<Feil> {
            vedtaksbrevService.lagBeslutterBrev(saksbehandling(fagsak,
                                                               behandlingForBeslutter.copy(steg = StegType.VILKÅR)))
        }
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig status`() {
        assertThrows<Feil> {
            vedtaksbrevService.lagBeslutterBrev(saksbehandling(fagsak,
                                                               behandlingForBeslutter.copy(status =
                                                                                           BehandlingStatus.FERDIGSTILT)))
        }

        assertThrows<Feil> {
            vedtaksbrevService.lagBeslutterBrev(saksbehandling(fagsak,
                                                               behandling.copy(status = BehandlingStatus.UTREDES)))
        }
    }

    @Test
    internal fun `lagSaksbehandlerBrev skal kaste feil når behandling er låst for videre behandling`() {
        assertThrows<Feil> {
            vedtaksbrevService
                    .lagSaksbehandlerSanitybrev(saksbehandling(fagsak,
                                                               behandlingForBeslutter.copy(status =
                                                                                           BehandlingStatus.FERDIGSTILT)),
                                                TextNode(""),
                                                "")
        }
    }

    private val behandlingForBeslutter = behandling(fagsak,
                                                    status = BehandlingStatus.FATTER_VEDTAK,
                                                    steg = StegType.BESLUTTE_VEDTAK)

    private val behandlingForSaksbehandler = behandling(fagsak,
                                                        status = BehandlingStatus.UTREDES,
                                                        steg = StegType.SEND_TIL_BESLUTTER)

    private fun lagVedtaksbrev(brevmal: String, saksbehandlerIdent: String = "123") = Vedtaksbrev(behandling.id,
                                                                                                  "123",
                                                                                                  brevmal,
                                                                                                  "Saksbehandler Signatur",
                                                                                                  null,
                                                                                                  null, "",
                                                                                                  saksbehandlerIdent,
                                                                                                  "")

    private fun lagVedtaksbrevFritekstDto() = VedtaksbrevFritekstDto("Innvilget",
                                                                     listOf(FrittståendeBrevAvsnitt("Deloverskrift", "Innhold")),
                                                                     behandling.id)

    @Test
    internal fun `JsonNode toString fungerer som forventet`() {
        val json = """{"name":"John"}"""
        assertThat(objectMapper.readTree(json).toString()).isEqualTo(json)
    }

}