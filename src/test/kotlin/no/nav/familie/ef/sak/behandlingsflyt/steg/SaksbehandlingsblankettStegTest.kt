package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.UUID

internal class SaksbehandlingsblankettStegTest {


    private val blankettServiceMock = mockk<BlankettService>()
    private val taskRepositoryMock = mockk<TaskRepository>()
    private val arbeidsfordelingServiceMock = mockk<ArbeidsfordelingService>()
    private val totrinnskontrollServiceMock = mockk<TotrinnskontrollService>()
    private val journalpostClientMock = mockk<JournalpostClient>()
    private val behandlingServiceMock = mockk<BehandlingService>()
    private val fagsakServiceMock = mockk<FagsakService>()

    private val saksbehandlingsblankettSteg = SaksbehandlingsblankettSteg(blankettService = blankettServiceMock,
                                                                          taskRepository = taskRepositoryMock,
                                                                          arbeidsfordelingService = arbeidsfordelingServiceMock,
                                                                          totrinnskontrollService = totrinnskontrollServiceMock,
                                                                          journalpostClient = journalpostClientMock,
                                                                          behandlingService = behandlingServiceMock,
                                                                          fagsakService = fagsakServiceMock
    )


    @BeforeEach
    internal fun setUp() {
        MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
        every { blankettServiceMock.lagBlankett(any()) } returns "123".toByteArray()
        every { fagsakServiceMock.hentFagsak(any()) } returns fagsak(fagsakpersoner(setOf("12345678912")))
        val arkiverDokumentResponse =
                ArkiverDokumentResponse(journalpostId = "12341234", ferdigstilt = true, dokumenter = listOf())
        every { journalpostClientMock.arkiverDokument(any(), any()) } returns arkiverDokumentResponse
        every { arbeidsfordelingServiceMock.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns "4489"
        every { totrinnskontrollServiceMock.hentBeslutter(any()) } returns "BeslutterPerson"
        every { behandlingServiceMock.leggTilBehandlingsjournalpost(any(), any(), any()) } just Runs
        every { taskRepositoryMock.save(any()) } answers { firstArg() }

    }

    @AfterEach
    internal fun tearDown() {
        MDC.remove(MDCConstants.MDC_CALL_ID)
    }

    @Test
    internal fun `skal opprette, lagre og arkivere blankett for førstegangsbehandling`() {
        val behandling = behandling(type = BehandlingType.FØRSTEGANGSBEHANDLING)
        saksbehandlingsblankettSteg.utførSteg(behandling, null)
        verify(exactly = 1) { blankettServiceMock.lagBlankett(any()) }
        verify(exactly = 1) { journalpostClientMock.arkiverDokument(any(), any()) }
    }

    @Test
    internal fun `skal journalføre blankett hvis det er revurdering`() {
        val behandling = behandling(type = BehandlingType.REVURDERING)
        saksbehandlingsblankettSteg.utførSteg(behandling, null)
        verify(exactly = 1) { blankettServiceMock.lagBlankett(any()) }
        verify(exactly = 1) { journalpostClientMock.arkiverDokument(any(), any()) }
    }

}