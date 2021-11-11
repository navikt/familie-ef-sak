package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        every { blankettServiceMock.lagBlankett(any()) } returns "123".toByteArray()
    }

    @Test
    internal fun `skal opprette og lagre blankett lokalt`() {
        val behandling = behandling()
        saksbehandlingsblankettSteg.utførSteg(behandling, null)

    }

    @Test
    internal fun `skal ikke journalføre blankett hvis det er revurdering`() {
        TODO("Not yet implemented")
    }

    @Test
    internal fun `skal journalføre blankett for førstegangsbehandling`() {
        TODO("Not yet implemented")
    }
}