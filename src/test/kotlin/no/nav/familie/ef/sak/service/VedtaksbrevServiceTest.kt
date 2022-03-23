package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_SIGNATUR_PLACEHOLDER
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class VedtaksbrevServiceTest {

    private val fagsak = fagsak(setOf(PersonIdent("12345678910")))
    private val behandling = behandling(fagsak)

    private val brevClient = mockk<BrevClient>()
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val brevsignaturService = mockk<BrevsignaturService>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()

    private val vedtaksbrevService =
            VedtaksbrevService(brevClient,
                               vedtaksbrevRepository,
                               personopplysningerService,
                               brevsignaturService,
                               familieDokumentClient)

    private val vedtaksbrev: Vedtaksbrev = lagVedtaksbrev("malnavn")
    private val beslutterNavn = "456"
    private val fritekstBrevDto = lagVedtaksbrevFritekstDto()

    @BeforeEach
    fun setUp(){
        mockBrukerContext(beslutterNavn)
        val signaturDto = SignaturDto(beslutterNavn, "enhet", false)
        every { brevsignaturService.lagSignaturMedEnhet(any<Saksbehandling>()) } returns signaturDto
    }

    @AfterEach
    fun tearDown(){
        clearBrukerContext()
    }

    @Test
    internal fun `skal legge på signatur og lage pdf ved lagBeslutterBrev`() {
        val vedtaksbrevSlot = slot<Vedtaksbrev>()


        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { brevClient.genererBrev(any()) } returns "enPdf".toByteArray()
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } returns vedtaksbrev

        vedtaksbrevService.lagBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))

        assertThat(vedtaksbrevSlot.captured.besluttersignatur).isEqualTo(beslutterNavn)
        assertThat(vedtaksbrevSlot.captured).usingRecursiveComparison()
                .ignoringFields("besluttersignatur", "beslutterPdf", "beslutterident", "enhet")
                .isEqualTo(vedtaksbrev)
        assertThat(vedtaksbrevSlot.captured.saksbehandlerHtml).isEqualTo(null)
        assertThat(vedtaksbrevSlot.captured.saksbehandlerBrevrequest).isNotBlank()

    }

    @Test
    internal fun `skal legge på signatur og lage pdf ved lagSaksbehandlerFritekstbrev`() {
        val vedtaksbrevSlot = slot<Vedtaksbrev>()

        val ident = "12345678910"
        val gjeldendeNavn = "Navn Navnesen"
        val navnMap = mapOf(ident to gjeldendeNavn)

        every { personopplysningerService.hentGjeldeneNavn(any()) } returns navnMap
        every { vedtaksbrevRepository.existsById(any()) } returns true
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } returns vedtaksbrev
        every { brevClient.genererBrev(any()) } returns "123".toByteArray()

        vedtaksbrevService.lagSaksbehandlerFritekstbrev(fritekstBrevDto, saksbehandling(fagsak, behandlingForSaksbehandler))
        assertThat(vedtaksbrevSlot.captured.saksbehandlersignatur).isEqualTo(beslutterNavn)

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

    private fun lagVedtaksbrev(brevmal: String, saksbehandlerIdent: String = "123") = Vedtaksbrev(behandlingId = behandling.id,
                                                                                                  saksbehandlerBrevrequest = "123",
                                                                                                  saksbehandlerHtml = null,
                                                                                                  brevmal = brevmal,
                                                                                                  saksbehandlersignatur = "Saksbehandler Signatur",
                                                                                                  besluttersignatur = null,
                                                                                                  beslutterPdf = null, enhet = "",
                                                                                                  saksbehandlerident = saksbehandlerIdent,
                                                                                                  beslutterident = "")

    private fun lagVedtaksbrevFritekstDto() = VedtaksbrevFritekstDto("Innvilget",
                                                                     listOf(FrittståendeBrevAvsnitt("Deloverskrift", "Innhold")),
                                                                     behandling.id)

    @Test
    internal fun `JsonNode toString fungerer som forventet`() {
        val json = """{"name":"John"}"""
        assertThat(objectMapper.readTree(json).toString()).isEqualTo(json)
    }

    @Test
    fun `skal kaste feil hvis saksbehandlerHtml ikke inneholder placeholder for besluttersignatur`(){
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlerHtml = "html uten placeholder")


        val feilmelding = assertThrows<Feil> {
            vedtaksbrevService.lagBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))
        }.message
        assertThat(feilmelding).isEqualTo("Brev-HTML mangler placeholder for besluttersignatur")
    }

    @Test
    fun `Skal erstatte placeholder med besluttersignatur`(){

        val htmlSlot = slot<String>()

        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlerHtml = "html med placeholder $BESLUTTER_SIGNATUR_PLACEHOLDER og en liten avslutning")
        every { vedtaksbrevRepository.update(any()) } returns vedtaksbrev
        every { familieDokumentClient.genererPdfFraHtml(capture(htmlSlot)) } returns "123".toByteArray()


        vedtaksbrevService.lagBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))

        assertThat(htmlSlot.captured).isEqualTo("html med placeholder $beslutterNavn og en liten avslutning")
    }

    @Test
    fun `Skal lage pdf gitt html fra familie-brev`(){

        val vedtaksbrevSlot = slot<Vedtaksbrev>()

        val html = "html"

        every { brevClient.genererHtml(any(), any(), any(), any(), any()) } returns html
        every { vedtaksbrevRepository.existsById(any()) } returns false
        every { vedtaksbrevRepository.insert(capture(vedtaksbrevSlot)) } returns vedtaksbrev
        every { familieDokumentClient.genererPdfFraHtml(any()) } returns "123".toByteArray()

        vedtaksbrevService.lagSaksbehandlerSanitybrev(saksbehandling(fagsak, behandling), objectMapper.createObjectNode(), "brevmal")

        assertThat(vedtaksbrevSlot.captured.saksbehandlerHtml).isEqualTo(html)
        assertThat(vedtaksbrevSlot.captured.saksbehandlerBrevrequest).isEmpty()
    }



}