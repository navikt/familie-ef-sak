package no.nav.familie.ef.sak.behandling.oppgavekontroll

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.testutil.kjørSomLeader
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.IsoFields

class BehandlingsoppgaveServiceTest {
    private val taskService = mockk<TaskService>(relaxed = true)
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val oppgaveService: OppgaveService = mockk()

    val behandlingsoppgaveService = BehandlingsoppgaveService(taskService, behandlingService, fagsakService, oppgaveService)

    @Test
    internal fun `Skal bruke år og ukenummer når vi ser etter allerede opprettet task`() {
        val payloadOgUnikNøkkel = payloadMedÅrOgUkenummer()
        kjørSomLeader {
            behandlingsoppgaveService.opprettTask()
        }
        verify(exactly = 1) { taskService.finnTaskMedPayloadOgType(payloadOgUnikNøkkel, BehandlingUtenOppgaveTask.TYPE)}
    }

    @Test
    internal fun `Skal ikke opprette ny task om det finnes en fra før`() {
        val payloadOgUnikNøkkel = payloadMedÅrOgUkenummer()
        val task = BehandlingUtenOppgaveTask.opprettTask(payloadOgUnikNøkkel)
        kjørSomLeader {
            every { taskService.finnTaskMedPayloadOgType(payloadOgUnikNøkkel, BehandlingUtenOppgaveTask.TYPE) } returns task
            behandlingsoppgaveService.opprettTask()
        }

        verify(exactly = 0) { taskService.save(any<Task>()) }
    }

    @Test
    internal fun `Skal lage task om det ikke finnes en fra før`() {
        val payloadOgUnikNøkkel = payloadMedÅrOgUkenummer()
        val task = BehandlingUtenOppgaveTask.opprettTask(payloadOgUnikNøkkel)

        kjørSomLeader {
            every { taskService.finnTaskMedPayloadOgType(payloadOgUnikNøkkel, BehandlingUtenOppgaveTask.TYPE) } returns null
            behandlingsoppgaveService.opprettTask()
            every { taskService.finnTaskMedPayloadOgType(payloadOgUnikNøkkel, BehandlingUtenOppgaveTask.TYPE) } returns task
            behandlingsoppgaveService.opprettTask()
            behandlingsoppgaveService.opprettTask()
        }

        verify(exactly = 1) { taskService.save(any<Task>()) }
    }

    @Test
    internal fun `Skal ikke lage task dersom ikke leder`() {

        kjørSomLeader(erLeder = false) {
            behandlingsoppgaveService.opprettTask()
        }

        verify(exactly = 0)  { taskService.finnTaskMedPayloadOgType(any(), any()) }
        verify(exactly = 0) { taskService.save(any()) }
    }

    private fun payloadMedÅrOgUkenummer(): String {
        val ukenummer = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = LocalDate.now().year
        val payloadOgUnikNøkkel = "År:$year Uke:$ukenummer"
        return payloadOgUnikNøkkel
    }

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

        every { oppgaveService.finnBehandleSakOppgaver(any()) } returns
            listOf(
                FinnOppgaveResponseDto(1, listOf(Oppgave(saksreferanse = fagsakMedOppgave.eksternId.toString()))),
            )

        val antallÅpneBehandlingerUtenOppgave = behandlingsoppgaveService.antallÅpneBehandlingerUtenOppgave()

        Assertions.assertThat(antallÅpneBehandlingerUtenOppgave).isEqualTo(2)
    }
}
