package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.lang.IllegalStateException
import java.util.*

internal class VedtaksbrevServiceTest : OppslagSpringRunnerTest() {


    @Autowired lateinit var vedtaksbrevService: VedtaksbrevService
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var vedtaksbrevRepository: VedtaksbrevRepository

    val brevClientMock = mockk<BrevClient>()
    val behandlingServiceMock = mockk<BehandlingService>()
    val fagsakServiceMock = mockk<FagsakService>()
    val personServiceMock = mockk<PersonService>()
    val journalpostClientMock = mockk<JournalpostClient>()
    val arbeidsfordelingServiceMock = mockk<ArbeidsfordelingService>()
    val vedtaksbrevRepositoryMock = mockk<VedtaksbrevRepository>()
    val familieIntegrasjonerClientMock = mockk<FamilieIntegrasjonerClient>()

    private val fagsak = fagsak(setOf(FagsakPerson("")))
    private val behandling = behandling(fagsak)

    internal fun lagServiceMedMocker() = VedtaksbrevService(brevClientMock,
                                                            vedtaksbrevRepositoryMock,
                                                            behandlingServiceMock,
                                                            fagsakServiceMock,
                                                            personServiceMock,
                                                            journalpostClientMock,
                                                            arbeidsfordelingServiceMock,
                                                            familieIntegrasjonerClientMock)

    @BeforeEach
    internal fun setUp() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `skal ikke kunne lage endelig brev hvis utkast ikke finnes`() {
        assertThrows<IllegalStateException> { vedtaksbrevService.lagreEndeligBrev(behandlingId = behandling.id) }
    }

    @Test
    fun `skal lage endelig brev basert på utkast`() {
        val utkast = vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)
        val endelig = vedtaksbrevService.lagreEndeligBrev(behandlingId = behandling.id)

        assertThat(utkast).usingRecursiveComparison().ignoringFields("brevRequest", "pdf").isEqualTo(endelig)
        assertThat(utkast.utkastBrevRequest).usingRecursiveComparison()
                .ignoringFields("signaturBeslutter")
                .isEqualTo(endelig.brevRequest)
    }

    @Test
    fun `utkast skal lagres i databasen`() {
        val utkast = vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)
        val forventetRequest = vedtaksbrevService.lagBrevRequest(behandlingId = behandling.id)

        assertThat(utkast).isEqualTo(vedtaksbrevRepository.findByIdOrThrow(behandling.id))
        assertThat(forventetRequest).isEqualTo(vedtaksbrevRepository.findByIdOrThrow(behandling.id).utkastBrevRequest)
        assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).utkastPdf).isNotNull
    }

    @Test
    fun `endelig brev skal lagres i databasen`() {
        vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)
        val endelig = vedtaksbrevService.lagreEndeligBrev(behandlingId = behandling.id)

        assertThat(endelig).isEqualTo(vedtaksbrevRepository.findByIdOrThrow(behandling.id))
        assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).pdf).isNotNull
    }

    @Test
    fun `endelig brev skal ikke lagres ved generering av utkast`() {
        vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)

        assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).brevRequest).isNull()
        assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).pdf).isNull()
    }

    @Test
    internal fun `skal journalføre vedtaksbrev`() {
        val brevClient = mockk<BrevClient>()
        val behandlingService = mockk<BehandlingService>()
        val fagsakService = mockk<FagsakService>()
        val personService = mockk<PersonService>()
        val journalpostClient = mockk<JournalpostClient>()
        val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
        val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
        val familieIntegrasjonerClient = mockk<FamilieIntegrasjonerClient>()

        val vedtaksbrev = Vedtaksbrev(behandling.id, mockk(), null, Fil("123".toByteArray()), Fil("123".toByteArray()))

        val vedtaksbrevService = VedtaksbrevService(brevClient,
                                                    vedtaksbrevRepository,
                                                    behandlingService,
                                                    fagsakService,
                                                    personService,
                                                    journalpostClient,
                                                    arbeidsfordelingService,
                                                    familieIntegrasjonerClient)

        val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()


        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet("4321", "enhetNavn")
        every { fagsakService.hentFaksakForBehandling(behandling.id) } returns fagsak
        every { vedtaksbrevRepository.findById(behandling.id) } returns Optional.of(vedtaksbrev)
        every {
            journalpostClient.arkiverDokument(capture(arkiverDokumentRequestSlot))
        } returns ArkiverDokumentResponse("1234", true)

        vedtaksbrevService.journalførVedtaksbrev(behandling.id)

        assertThat(arkiverDokumentRequestSlot.captured.fnr).isEqualTo(fagsak.hentAktivIdent())
        assertThat(arkiverDokumentRequestSlot.captured.fagsakId).isEqualTo(fagsak.eksternId.id.toString())
    }

    @Test
    internal fun `distribuerBrev skal distribuere brev`() {
        val journalpostId = "5555"
        val vedtaksbrevService = lagServiceMedMocker()

        every { familieIntegrasjonerClientMock.distribuerBrev(any()) } returns "9876"


        vedtaksbrevService.distribuerVedtaksbrev(behandling.id, journalpostId)

        verify { familieIntegrasjonerClientMock.distribuerBrev(journalpostId) }

    }

}