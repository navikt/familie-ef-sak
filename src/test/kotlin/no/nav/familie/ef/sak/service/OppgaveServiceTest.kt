package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.integration.OppgaveClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.EksternFagsakId
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Tema
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.net.URI
import java.time.LocalDate
import java.util.UUID

internal class OppgaveServiceTest {


    private val oppgaveClient = mockk<OppgaveClient>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val pdlClient = mockk<PdlClient>()

    private val oppgaveService =
            OppgaveService(oppgaveClient,
                           behandlingRepository,
                           fagsakRepository,
                           oppgaveRepository,
                           arbeidsfordelingService,
                           pdlClient,
                           URI.create("https://ensligmorellerfar.intern.nav.no/oppgavebenk"))

    @Test
    fun `Opprett oppgave skal samle data og opprette en ny oppgave basert på fagsak, behandling, fnr og enhet`() {
        val aktørIdentFraPdl = "AKTØERIDENT"
        every { behandlingRepository.findByIdOrNull(BEHANDLING_ID) } returns lagTestBehandling()
        every { fagsakRepository.findByIdOrNull(FAGSAK_ID) } returns lagTestFagsak()
        every { behandlingRepository.update(any()) } returns lagTestBehandling()
        every { oppgaveRepository.insert(any()) } returns lagTestOppgave()
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet(enhetId = ENHETSNUMMER,
                                                                                             enhetNavn = ENHETSNAVN)
        val slot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(slot)) } returns GSAK_OPPGAVE_ID
        every { pdlClient.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørIdentFraPdl, false)))

        oppgaveService.opprettOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak, FRIST_FERDIGSTILLELSE_BEH_SAK)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_EKSTERN_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = aktørIdentFraPdl, gruppe = IdentGruppe.AKTOERID))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad.value)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.ENF)
        assertThat(slot.captured.beskrivelse).contains("https://ensligmorellerfar.intern.nav.no/oppgavebenk")
    }

    @Test
    fun `Skal kunne hente oppgave gitt en ID`() {
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave()
        val oppgave = oppgaveService.hentOppgave(GSAK_OPPGAVE_ID)

        assertThat(oppgave.id).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Skal hente oppgaver gitt en filtrering`() {
        every { oppgaveClient.hentOppgaver(any()) } returns lagFinnOppgaveResponseDto()
        val respons = oppgaveService.hentOppgaver(FinnOppgaveRequest(tema = Tema.ENF))

        assertThat(respons.antallTreffTotalt).isEqualTo(1)
        assertThat(respons.oppgaver.first().id).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave`() {
        every { behandlingRepository.findByIdOrNull(BEHANDLING_ID) } returns mockk {}
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillBehandleOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak)
        assertThat(slot.captured).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave feiler fordi den ikke finner oppgave på behandlingen`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { oppgaveRepository.insert(any()) } returns lagTestOppgave()
        every { behandlingRepository.findByIdOrNull(BEHANDLING_ID) } returns mockk {}

        Assertions.assertThatThrownBy { oppgaveService.ferdigstillBehandleOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak) }
                .hasMessage("Finner ikke oppgave for behandling $BEHANDLING_ID")
                .isInstanceOf(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `Fordel oppgave skal tildele oppgave til saksbehandler`() {
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()
        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), capture(saksbehandlerSlot)) } returns GSAK_OPPGAVE_ID

        oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID)

        assertThat(GSAK_OPPGAVE_ID).isEqualTo(oppgaveSlot.captured)
        assertThat(SAKSBEHANDLER_ID).isEqualTo(saksbehandlerSlot.captured)
    }

    @Test
    fun `Tilbakestill oppgave skal nullstille tildeling på oppgave`() {
        val oppgaveSlot = slot<Long>()
        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), any()) } returns GSAK_OPPGAVE_ID

        oppgaveService.tilbakestillFordelingPåOppgave(GSAK_OPPGAVE_ID)

        assertThat(GSAK_OPPGAVE_ID).isEqualTo(oppgaveSlot.captured)
        verify(exactly = 1) { oppgaveClient.fordelOppgave(any(), null) }
    }

    private fun lagTestBehandling(): Behandling {
        return Behandling(fagsakId = FAGSAK_ID,
                          type = BehandlingType.FØRSTEGANGSBEHANDLING,
                          status = BehandlingStatus.OPPRETTET,
                          steg = StegType.REGISTRERE_OPPLYSNINGER,
                          resultat = BehandlingResultat.IKKE_SATT)
    }

    private fun lagTestFagsak(): Fagsak {
        return Fagsak(id = FAGSAK_ID,
                      stønadstype = Stønadstype.OVERGANGSSTØNAD,
                      eksternId = EksternFagsakId(FAGSAK_EKSTERN_ID),
                      søkerIdenter = setOf(FagsakPerson(ident = FNR)))
    }

    private fun lagTestOppgave(): Oppgave {
        return Oppgave(behandlingId = BEHANDLING_ID, type = Oppgavetype.BehandleSak, gsakOppgaveId = GSAK_OPPGAVE_ID)
    }

    private fun lagEksternTestOppgave(): no.nav.familie.kontrakter.felles.oppgave.Oppgave {
        return no.nav.familie.kontrakter.felles.oppgave.Oppgave(id = GSAK_OPPGAVE_ID)
    }

    private fun lagFinnOppgaveResponseDto(): FinnOppgaveResponseDto {
        return FinnOppgaveResponseDto(antallTreffTotalt = 1,
                                      oppgaver = listOf(lagEksternTestOppgave())
        )
    }

    companion object {

        private val FAGSAK_ID = UUID.fromString("1242f220-cad3-4640-95c1-190ec814c91e")
        private const val FAGSAK_EKSTERN_ID = 98765L
        private const val GSAK_OPPGAVE_ID = 12345L
        private val BEHANDLING_ID = UUID.fromString("1c4209bd-3217-4130-8316-8658fe300a84")
        private const val ENHETSNUMMER = "enhetnr"
        private const val ENHETSNAVN = "enhetsnavn"
        private const val FNR = "11223312345"
        private const val SAKSBEHANDLER_ID = "Z999999"
        private val FRIST_FERDIGSTILLELSE_BEH_SAK = LocalDate.now().plusDays(1)
    }
}