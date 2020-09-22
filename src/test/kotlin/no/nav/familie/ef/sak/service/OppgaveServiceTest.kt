package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.integration.OppgaveClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.*
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
        every { behandlingRepository.save(any<Behandling>()) } returns lagTestBehandling()
        every { oppgaveRepository.save(any<Oppgave>()) } returns lagTestOppgave()
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any<UUID>(), any<Oppgavetype>())
        } returns null
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet(enhetId = ENHETSNUMMER,
                                                                                             enhetNavn = ENHETSNAVN)
        val slot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(slot)) } returns OPPGAVE_ID

        oppgaveService.opprettOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak, FRIST_FERDIGSTILLELSE_BEH_SAK)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = FNR, gruppe = IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(slot.captured.behandlingstema).isEqualTo(OppgaveService.Behandlingstema.OVERGANGSSTØNAD.kode)
        assertThat(slot.captured.fristFerdigstillelse).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.ENF)
        assertThat(slot.captured.beskrivelse).contains("https://ensligmorellerfar.prod-fss.nais.io/fagsak/$FAGSAK_ID")
    }

    @Test
    fun `Skal kunne hente oppgave gitt en ID`() {
        every { oppgaveClient.finnOppgaveMedId(any<Long>()) } returns lagEksternTestOppgave()
        val oppgave = oppgaveService.hentOppgave(OPPGAVE_ID_L)

        assertThat(oppgave.id).isEqualTo(OPPGAVE_ID_L)
    }

    @Test
    fun `Skal hente oppgaver gitt en filtrering`() {
        every { oppgaveClient.hentOppgaver(any<FinnOppgaveRequest>()) } returns lagFinnOppgaveResponseDto()
        val respons = oppgaveService.hentOppgaver(FinnOppgaveRequest(tema = Tema.ENF))

        assertThat(respons.antallTreffTotalt).isEqualTo(1)
        assertThat(respons.oppgaver.first().id).isEqualTo(OPPGAVE_ID_L)
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
                id= OPPGAVE_ID_L
        )
    }

    private fun lagFinnOppgaveResponseDto(): FinnOppgaveResponseDto {
        return FinnOppgaveResponseDto(antallTreffTotalt =  1,
                oppgaver = listOf(lagEksternTestOppgave())
        )
    }

    companion object {

        private val FAGSAK_ID = UUID.fromString("1242f220-cad3-4640-95c1-190ec814c91e")
        private val GSAK_ID = "12345"
        private val BEHANDLING_ID = UUID.fromString("1c4209bd-3217-4130-8316-8658fe300a84")
        private const val OPPGAVE_ID = "42"
        private const val OPPGAVE_ID_L = 42L
        private const val ENHETSNUMMER = "enhetnr"
        private const val ENHETSNAVN = "enhetsnavn"
        private const val FNR = "11223312345"
        private const val SAKSBEHANDLER_ID = "Z999999"
        private val FRIST_FERDIGSTILLELSE_BEH_SAK = LocalDate.now().plusDays(1)
    }
}