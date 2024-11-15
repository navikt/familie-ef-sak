package no.nav.familie.ef.sak.journalføring

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequestV2
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsaksjon
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsårsak
import no.nav.familie.ef.sak.journalføring.dto.NyAvsender
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

    private val service = JournalføringKlageService(fagsakService, oppgaveService, journalpostService, klageService)

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

        justRun { klageService.validerOgOpprettKlage(any<Fagsak>(), any()) }
        justRun { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any(), any(), any()) }
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
            service.fullførJournalpostV2(lagRequest(), lagjournalpost(LocalDate.now()))

            verifyKall()
        }

        @Test
        internal fun `happy case - gjelder tilbakekreving`() {
            service.fullførJournalpostV2(lagRequest(årsak = Journalføringsårsak.KLAGE_TILBAKEKREVING), lagjournalpost(LocalDate.now()))

            verifyKall()
        }

        @Test
        internal fun `må sende inn mottattDato hvis journalposten mangler mottattDato`() {
            assertThatThrownBy {
                service.fullførJournalpostV2(lagRequest(), lagjournalpost())
            }.hasMessageContaining("Mangler dato mottatt")
        }
    }

    @Nested
    inner class EskisterendeBehandling {
        @Test
        internal fun `happy case`() {
            service.fullførJournalpostV2(
                lagRequest(aksjon = Journalføringsaksjon.JOURNALFØR_PÅ_FAGSAK),
                lagjournalpost(LocalDate.now()),
            )

            verifyKall(opprettKlageKall = 0)
        }
    }

    private fun verifyKall(
        opprettKlageKall: Int = 1,
    ) {
        verify(exactly = opprettKlageKall) { klageService.validerOgOpprettKlage(any<Fagsak>(), any()) }
        verify { journalpostService.oppdaterOgFerdigstillJournalpost(any(), any(), any(), any(), any(), any(), any()) }
        verify { oppgaveService.ferdigstillOppgave(any()) }
    }

    private fun lagRequest(
        aksjon: Journalføringsaksjon = Journalføringsaksjon.OPPRETT_BEHANDLING,
        årsak: Journalføringsårsak = Journalføringsårsak.KLAGE,
    ) = JournalføringRequestV2(aksjon = aksjon, årsak = årsak, dokumentTitler = emptyMap(), fagsakId = fagsakId, oppgaveId = oppgaveId, journalførendeEnhet = "enhet", nyAvsender = NyAvsender(erBruker = false, navn = "Fjas", personIdent = null))

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
