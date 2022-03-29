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
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.VedtaksbrevRepository
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
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
import org.springframework.http.HttpStatus.BAD_REQUEST

internal class VedtaksbrevServiceTest {

    private val fagsak = fagsak(setOf(PersonIdent("12345678910")))
    private val behandling = behandling(fagsak)

    private val brevClient = mockk<BrevClient>()
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val brevsignaturService = mockk<BrevsignaturService>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val vedtaksbrevService =
            VedtaksbrevService(brevClient,
                               vedtaksbrevRepository,
                               personopplysningerService,
                               brevsignaturService,
                               familieDokumentClient,
                               featureToggleService)

    private val vedtaksbrev: Vedtaksbrev = lagVedtaksbrev("malnavn")
    private val beslutterNavn = "456"
    private val fritekstBrevDto = lagVedtaksbrevFritekstDto()

    @BeforeEach
    fun setUp() {
        mockBrukerContext(beslutterNavn)
        val signaturDto = SignaturDto(beslutterNavn, "enhet", false)
        every { brevsignaturService.lagSignaturMedEnhet(any<Saksbehandling>()) } returns signaturDto
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
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
        every { brevClient.genererHtmlFritekstbrev(any(), any(), any()) } returns "html"
        every { familieDokumentClient.genererPdfFraHtml(any()) } returns "123".toByteArray()

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
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev


        val feil = assertThrows<Feil> {
            vedtaksbrevService.lagEndeligBeslutterbrev(saksbehandling(fagsak,
                                                                      behandlingForBeslutter.copy(steg = StegType.VILKÅR)))
        }
        assertThat(feil.message).contains("Behandling er i feil steg")
        assertThat(feil.httpStatus).isEqualTo(BAD_REQUEST)
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig status`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev

        val feilFerdigstilt = assertThrows<Feil> {
            vedtaksbrevService.lagEndeligBeslutterbrev(saksbehandling(fagsak,
                                                                      behandlingForBeslutter.copy(status =
                                                                                                  BehandlingStatus.FERDIGSTILT)))
        }
        assertThat(feilFerdigstilt.httpStatus).isEqualTo(BAD_REQUEST)
        assertThat(feilFerdigstilt.message).contains("Behandling er i feil steg")


        val feilUtredes = assertThrows<Feil> {
            vedtaksbrevService.lagEndeligBeslutterbrev(saksbehandling(fagsak,
                                                                      behandling.copy(status = BehandlingStatus.UTREDES)))
        }
        assertThat(feilUtredes.httpStatus).isEqualTo(BAD_REQUEST)
        assertThat(feilUtredes.message).contains("Behandling er i feil steg")

    }

    @Test
    internal fun `skal kaste feil når det finnes beslutterpdf i forveien`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(beslutterPdf = Fil("123".toByteArray()))
        every { featureToggleService.isEnabled("familie.ef.sak.skal-validere-beslutterpdf-er-null") } returns true

        val feil = assertThrows<Feil> {
            vedtaksbrevService.lagEndeligBeslutterbrev(saksbehandling(fagsak,
                                                                      behandlingForBeslutter))
        }
        assertThat(feil.message).isEqualTo("Det finnes allerede et beslutterbrev")
    }

    @Test
    internal fun `skal lage brev med innlogget beslutterident beslutterident `() {

        val beslutterIdent = SikkerhetContext.hentSaksbehandler(true)
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(beslutterident = "tilfeldigvisFeilIdent")
        every { featureToggleService.isEnabled("familie.ef.sak.skal-validere-beslutterpdf-er-null") } returns true
        val brevSlot = slot<Vedtaksbrev>()
        every { vedtaksbrevRepository.update(capture(brevSlot)) } returns mockk()
        every { familieDokumentClient.genererPdfFraHtml(any()) } returns "brev".toByteArray()
        // Når
        vedtaksbrevService.lagEndeligBeslutterbrev(saksbehandling(fagsak, behandlingForBeslutter))

        assertThat(beslutterIdent).isNotNull()
        assertThat(brevSlot.captured.beslutterident).isEqualTo(beslutterIdent)

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
                                                                                                  saksbehandlerHtml = "Brev med $BESLUTTER_SIGNATUR_PLACEHOLDER",
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
    fun `skal kaste feil hvis saksbehandlerHtml ikke inneholder placeholder for besluttersignatur`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlerHtml = "html uten placeholder")


        val feilmelding = assertThrows<Feil> {
            vedtaksbrevService.forhåndsvisBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))
        }.message
        assertThat(feilmelding).isEqualTo("Brev-HTML mangler placeholder for besluttersignatur")
    }

    @Test
    fun `Skal erstatte placeholder med besluttersignatur`() {

        val htmlSlot = slot<String>()

        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlerHtml = "html med placeholder $BESLUTTER_SIGNATUR_PLACEHOLDER og en liten avslutning")
        every { vedtaksbrevRepository.update(any()) } returns vedtaksbrev
        every { familieDokumentClient.genererPdfFraHtml(capture(htmlSlot)) } returns "123".toByteArray()


        vedtaksbrevService.forhåndsvisBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))

        assertThat(htmlSlot.captured).isEqualTo("html med placeholder $beslutterNavn og en liten avslutning")
    }

    @Test
    fun `Skal lage pdf gitt html fra familie-brev`() {

        val vedtaksbrevSlot = slot<Vedtaksbrev>()

        val html = "html"

        every { brevClient.genererHtml(any(), any(), any(), any(), any()) } returns html
        every { vedtaksbrevRepository.existsById(any()) } returns false
        every { vedtaksbrevRepository.insert(capture(vedtaksbrevSlot)) } returns vedtaksbrev
        every { familieDokumentClient.genererPdfFraHtml(any()) } returns "123".toByteArray()

        vedtaksbrevService.lagSaksbehandlerSanitybrev(saksbehandling(fagsak, behandling),
                                                      objectMapper.createObjectNode(),
                                                      "brevmal")

        assertThat(vedtaksbrevSlot.captured.saksbehandlerHtml).isEqualTo(html)
    }
}