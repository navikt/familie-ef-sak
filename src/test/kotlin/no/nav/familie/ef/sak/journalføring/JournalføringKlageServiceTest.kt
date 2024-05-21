package no.nav.familie.ef.sak.journalføring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.journalføring.dto.JournalføringKlageBehandling
import no.nav.familie.ef.sak.journalføring.dto.JournalføringKlageRequest
import no.nav.familie.ef.sak.klage.KlageService
import no.nav.familie.ef.sak.klage.dto.KlagebehandlingerDto
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class JournalføringKlageServiceTest {
    private val klageService = mockk<KlageService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val fagsakService = mockk<FagsakService>()
    private val journalpostService = mockk<JournalpostService>()
    private val taskService = mockk<TaskService>()

    private val service = JournalføringKlageService(fagsakService, oppgaveService, journalpostService, klageService, taskService)

    private val fagsak = fagsak()
    private val fagsakId = fagsak.id
    private val journalpostId = "journalpostId"
    private val oppgaveId = "123"

    private val klagebehandling =
        KlagebehandlingDto(
            id = UUID.randomUUID(),
            fagsakId = fagsak.id,
            status = BehandlingStatus.FERDIGSTILT,
            opprettet = LocalDateTime.now(),
            mottattDato = LocalDate.now(),
            resultat = null,
            årsak = null,
            vedtaksdato = null,
            klageinstansResultat = emptyList(),
        )

    @BeforeEach
    internal fun setUp() {
        every { journalpostService.hentJournalpost(journalpostId) } returns lagjournalpost(mottattDato = LocalDate.now())
        every { fagsakService.fagsakMedOppdatertPersonIdent(fagsakId) } returns fagsak
        every { klageService.hentBehandlinger(fagsak.fagsakPersonId) } returns
            KlagebehandlingerDto(listOf(klagebehandling), emptyList(), emptyList())

        justRun { klageService.opprettKlage(any<Fagsak>(), any(), any()) }
        justRun { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any(), any()) }
        justRun { oppgaveService.ferdigstillOppgave(any()) }
        mockBrukerContext()
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Nested
    inner class NyBehandling {
        @Test
        internal fun `happy case`() {
            service.fullførJournalpost(lagRequest(JournalføringKlageBehandling()), journalpostId)

            verifyKall()
        }

        @Test
        internal fun `må sende inn mottattDato hvis journalposten mangler mottattDato`() {
            every { journalpostService.hentJournalpost(journalpostId) } returns lagjournalpost(mottattDato = null)

            assertThatThrownBy {
                service.fullførJournalpost(lagRequest(JournalføringKlageBehandling()), journalpostId)
            }.hasMessageContaining("Mangler dato mottatt")
        }
    }

    @Nested
    inner class EskisterendeBehandling {
        @Test
        internal fun `happy case`() {
            service.fullførJournalpost(
                lagRequest(JournalføringKlageBehandling(behandlingId = klagebehandling.id)),
                journalpostId,
            )

            verifyKall(opprettKlageKall = 0)
        }

        @Test
        internal fun `skal opprette task for å oppdatere behandlingstema om klage gjelder tilbakekreving`() {
            val oppdaterTask =
                Task(
                    type = OppdaterOppgaveTilÅGjeldeTilbakekrevingTask.TYPE,
                    payload = UUID.randomUUID().toString(),
                )

            val taskSlot = slot<Task>()
            every { taskService.save(capture(taskSlot)) } returns oppdaterTask

            service.fullførJournalpost(
                lagRequest(JournalføringKlageBehandling(behandlingId = klagebehandling.id), klageGjelderTilbakekreving = true),
                journalpostId,
            )

            verifyKall(opprettKlageKall = 0, oppdaterOppgaveKall = 1)
            assertThat(taskSlot.captured.type).isEqualTo(OppdaterOppgaveTilÅGjeldeTilbakekrevingTask.TYPE)
            assertThat(taskSlot.captured.payload).isEqualTo(klagebehandling.id.toString())
        }

        @Test
        internal fun `feiler hvis klagebehandlinger ikke inneholder behandlingId som blir sendt inn`() {
            assertThatThrownBy {
                service.fullførJournalpost(
                    lagRequest(JournalføringKlageBehandling(behandlingId = UUID.randomUUID())),
                    journalpostId,
                )
            }.hasMessageContaining("mangler behandlingId")
        }
    }

    private fun verifyKall(
        opprettKlageKall: Int = 1,
        oppdaterOppgaveKall: Int = 0,
    ) {
        verify(exactly = opprettKlageKall) { klageService.opprettKlage(any<Fagsak>(), any(), any()) }
        verify { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any(), any()) }
        verify { oppgaveService.ferdigstillOppgave(any()) }
        verify(exactly = oppdaterOppgaveKall) { taskService.save(any()) }
    }

    private fun lagRequest(
        behandling: JournalføringKlageBehandling,
        klageGjelderTilbakekreving: Boolean = false,
    ) =
        JournalføringKlageRequest(emptyMap(), fagsakId, oppgaveId, behandling, "enhet", klageGjelderTilbakekreving)

    fun lagjournalpost(mottattDato: LocalDate? = null) =
        Journalpost(
            avsenderMottaker = AvsenderMottaker(null, null, "navn", null, true),
            journalpostId = journalpostId,
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.MOTTATT,
            tema = "ENF",
            behandlingstema = "behandlingstema",
            dokumenter = emptyList(),
            tittel = "Tittel",
            relevanteDatoer = mottattDato?.let { listOf(RelevantDato(it.atStartOfDay(), "DATO_REGISTRERT")) },
        )
}
