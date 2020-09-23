package no.nav.familie.ef.sak.service

import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.nav.familie.ef.sak.integration.OppgaveClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.*
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.*

internal class OppgaveServiceTest {


    private var oppgaveClient = mockk<OppgaveClient>()

    @MockK
    private var arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    @MockK
    private var behandlingRepository = mockk<BehandlingRepository>()

    @MockK
    private var fagsakRepository = mockk<FagsakRepository>()

    @MockK
    private var oppgaveRepository = mockk<OppgaveRepository>()

    private var oppgaveService =
            OppgaveService(oppgaveClient, behandlingRepository, fagsakRepository, oppgaveRepository, arbeidsfordelingService)


    @Test
    fun `Opprett oppgave skal samle data og opprette en ny oppgave basert på fagsak, behandling, fnr og enhet`() {
        every { behandlingRepository.findByIdOrNull(BEHANDLING_ID) } returns lagTestBehandling()
        every { fagsakRepository.findByIdOrNull(FAGSAK_ID) } returns lagTestFagsak()
        every { behandlingRepository.save(any()) } returns lagTestBehandling()
        every { oppgaveRepository.save(any()) } returns lagTestOppgave()
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet(enhetId = ENHETSNUMMER,
                                                                                             enhetNavn = ENHETSNAVN)
        val slot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(slot)) } returns GSAK_ID

        oppgaveService.opprettOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak, FRIST_FERDIGSTILLELSE_BEH_SAK)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = FNR, gruppe = IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad.value)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.ENF)
        assertThat(slot.captured.beskrivelse).contains("https://ensligmorellerfar.prod-fss.nais.io/fagsak/$FAGSAK_ID")
    }

    @Test
    fun `Skal kunne hente oppgave gitt en ID`() {
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave()
        val oppgave = oppgaveService.hentOppgave(GSAK_ID)

        assertThat(oppgave.id).isEqualTo(GSAK_ID)
    }

    @Test
    fun `Skal hente oppgaver gitt en filtrering`() {
        every { oppgaveClient.hentOppgaver(any()) } returns lagFinnOppgaveResponseDto()
        val respons = oppgaveService.hentOppgaver(FinnOppgaveRequest(tema = Tema.ENF))

        assertThat(respons.antallTreffTotalt).isEqualTo(1)
        assertThat(respons.oppgaver.first().id).isEqualTo(GSAK_ID)
    }

    @Test
    fun `Ferdigstill oppgave`() {
        every { behandlingRepository.findByIdOrNull(BEHANDLING_ID) } returns mockk {}
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.save(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak)
        assertThat(slot.captured).isEqualTo(GSAK_ID)
    }

    @Test
    fun `Ferdigstill oppgave feiler fordi den ikke finner oppgave på behandlingen`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { oppgaveRepository.save(any()) } returns lagTestOppgave()
        every { behandlingRepository.findByIdOrNull(BEHANDLING_ID) } returns mockk {}

        Assertions.assertThatThrownBy { oppgaveService.ferdigstillOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak) }
                .hasMessage("Finner ikke oppgave for behandling $BEHANDLING_ID")
                .isInstanceOf(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `Fordel oppgave skal tildele oppgave til saksbehandler`() {
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()
        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), capture(saksbehandlerSlot)) } returns OPPGAVE_ID

        oppgaveService.fordelOppgave(GSAK_ID, SAKSBEHANDLER_ID)

        assertThat(GSAK_ID).isEqualTo(oppgaveSlot.captured)
        assertThat(SAKSBEHANDLER_ID).isEqualTo(saksbehandlerSlot.captured)
    }

    @Test
    fun `Tilbakestill oppgave skal nullstille tildeling på oppgave`() {
        val oppgaveSlot = slot<Long>()
        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), any()) } returns OPPGAVE_ID

        oppgaveService.tilbakestillFordelingPåOppgave(GSAK_ID)

        assertThat(GSAK_ID).isEqualTo(oppgaveSlot.captured)
        verify(exactly = 1) { oppgaveClient.fordelOppgave(any(), null) }
    }

    private fun lagTestBehandling(): Behandling {
        return Behandling(
                fagsakId = FAGSAK_ID,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                opprinnelse = BehandlingOpprinnelse.MANUELL,
                status = BehandlingStatus.OPPRETTET,
                steg = BehandlingSteg.KOMMER_SENDERE)
    }

    private fun lagTestFagsak(): Fagsak {
        return Fagsak(id = FAGSAK_ID, stønadstype = Stønadstype.OVERGANGSSTØNAD).also {
            it.søkerIdenter = setOf(FagsakPerson(ident = FNR))
        }
    }

    private fun lagTestOppgave(): Oppgave {
        return Oppgave(behandlingId = BEHANDLING_ID, type = Oppgavetype.BehandleSak, gsakId = GSAK_ID)
    }

    private fun lagEksternTestOppgave(): no.nav.familie.kontrakter.felles.oppgave.Oppgave {
        return no.nav.familie.kontrakter.felles.oppgave.Oppgave(
                id = GSAK_ID
        )
    }

    private fun lagFinnOppgaveResponseDto(): FinnOppgaveResponseDto {
        return FinnOppgaveResponseDto(antallTreffTotalt = 1,
                                      oppgaver = listOf(lagEksternTestOppgave())
        )
    }

    companion object {

        private val FAGSAK_ID = UUID.fromString("1242f220-cad3-4640-95c1-190ec814c91e")
        private val GSAK_ID = 12345L
        private val BEHANDLING_ID = UUID.fromString("1c4209bd-3217-4130-8316-8658fe300a84")
        private const val OPPGAVE_ID = "42"
        private const val ENHETSNUMMER = "enhetnr"
        private const val ENHETSNAVN = "enhetsnavn"
        private const val FNR = "11223312345"
        private const val SAKSBEHANDLER_ID = "Z999999"
        private val FRIST_FERDIGSTILLELSE_BEH_SAK = LocalDate.now().plusDays(1)
    }
}