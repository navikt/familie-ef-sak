package no.nav.familie.ef.sak.behandlingsflyt.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OpprettOppgaveTaskTest {
    val oppgaveService = mockk<OppgaveService>()
    val behandlingService = mockk<BehandlingService>()

    val opprettOppgaveTask = OpprettOppgaveTask(oppgaveService, behandlingService)

    val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { oppgaveService.opprettOppgave(any(), any()) } returns 1L
        every { oppgaveService.opprettOppgaveUtenÅLagreIRepository(any(), any(), any(), any(), any()) } returns 2L
    }

    @Test
    fun `skal ikke opprette behandle sak oppgave om status er låst for videre redigering`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.FATTER_VEDTAK)

        val task = OpprettOppgaveTask.opprettTask(
            OpprettOppgaveTask.OpprettOppgaveTaskData(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.BehandleSak,
            ),
        )

        opprettOppgaveTask.doTask(task)
        verifyOpprettOppgaveMedLagringKall(0)
        verifyOpprettOppgaveUtenLagringKall(0)
    }

    @Test
    fun `skal opprette behandle sak oppgave om behandling ikke er låst for videre redigering`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.UTREDES)

        val task = OpprettOppgaveTask.opprettTask(
            OpprettOppgaveTask.OpprettOppgaveTaskData(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.BehandleSak,
            ),
        )

        opprettOppgaveTask.doTask(task)
        verifyOpprettOppgaveMedLagringKall(1)
        verifyOpprettOppgaveUtenLagringKall(0)
    }

    @Test
    fun `skal ikke lagre ned oppgave om oppgavetypen er vurder henvendelse`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.UTREDES)
        every {oppgaveService.lagOppgaveTekst(any())} returns ""

        val task = OpprettOppgaveTask.opprettTask(
            OpprettOppgaveTask.OpprettOppgaveTaskData(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.VurderHenvendelse,
                beskrivelse = ""
            ),
        )

        opprettOppgaveTask.doTask(task)
        verifyOpprettOppgaveMedLagringKall(0)
        verifyOpprettOppgaveUtenLagringKall(1)
    }

    private fun verifyOpprettOppgaveMedLagringKall(opprettOppgaveKall: Int) {
        verify(exactly = opprettOppgaveKall) { oppgaveService.opprettOppgave(any(), any()) }
    }

    private fun verifyOpprettOppgaveUtenLagringKall(opprettOppgaveKall: Int) {
        verify(exactly = opprettOppgaveKall) { oppgaveService.opprettOppgaveUtenÅLagreIRepository(any(), any(), any(), any(), any()) }
    }
}
