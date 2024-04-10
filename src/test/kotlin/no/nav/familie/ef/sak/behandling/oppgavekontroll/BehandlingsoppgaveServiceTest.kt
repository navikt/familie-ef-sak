package no.nav.familie.ef.sak.behandling.oppgavekontroll

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingsoppgaveServiceTest {

    private val taskService = mockk<TaskService>(relaxed = true)
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val oppgaveService: OppgaveService = mockk()

    val behandlingsoppgaveService = BehandlingsoppgaveService(taskService, behandlingService, fagsakService, oppgaveService)

    @Test
    internal fun `Skal returnere riktig antall uten oppgave når vi finner to`() {
        val returnGamleÅpneBehandlinger = listOf(behandling(), behandling(), behandling())
        every { behandlingService.hentUferdigeBehandlingerOpprettetFørDato(StønadType.OVERGANGSSTØNAD, any()) } returns returnGamleÅpneBehandlinger
        every { behandlingService.hentUferdigeBehandlingerOpprettetFørDato(StønadType.BARNETILSYN, any()) } returns emptyList()
        every { behandlingService.hentUferdigeBehandlingerOpprettetFørDato(StønadType.SKOLEPENGER, any()) } returns emptyList()

        val fagsakMedOppgave = fagsak(eksternId = 0)
        every { fagsakService.hentFagsakForBehandling(returnGamleÅpneBehandlinger[0].id) } returns fagsakMedOppgave
        every { fagsakService.hentFagsakForBehandling(returnGamleÅpneBehandlinger[1].id) } returns fagsak(eksternId = 1)
        every { fagsakService.hentFagsakForBehandling(returnGamleÅpneBehandlinger[2].id) } returns fagsak(eksternId = 2)

        every { oppgaveService.finnBehandleSakOppgaver(any()) } returns listOf(
            FinnOppgaveResponseDto(1, listOf(Oppgave(saksreferanse = fagsakMedOppgave.eksternId.toString()))),
        )

        val antallÅpneBehandlingerUtenOppgave = behandlingsoppgaveService.antallÅpneBehandlingerUtenOppgave()

        Assertions.assertThat(antallÅpneBehandlingerUtenOppgave).isEqualTo(2)
    }
}
