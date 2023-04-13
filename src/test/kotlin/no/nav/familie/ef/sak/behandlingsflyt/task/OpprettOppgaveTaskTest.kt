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
    }

    @Test
    fun `skal ikke opprette behandle sak oppgave om status er ugyldig`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.FATTER_VEDTAK)

        val task = OpprettOppgaveTask.opprettTask(OpprettOppgaveTask.OpprettOppgaveTaskData(
            behandlingId = behandlingId,
            oppgavetype = Oppgavetype.BehandleSak
        ))

        opprettOppgaveTask.doTask(task)
        verifyKall(0)
    }

    @Test
    fun `skal opprette behandle sak oppgave om status er gyldig`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.UTREDES)

        val task = OpprettOppgaveTask.opprettTask(OpprettOppgaveTask.OpprettOppgaveTaskData(
            behandlingId = behandlingId,
            oppgavetype = Oppgavetype.BehandleSak
        ))

        opprettOppgaveTask.doTask(task)
        verifyKall(1)
    }

    private fun verifyKall(opprettOppgaveKall: Int) {
        verify(exactly = opprettOppgaveKall) {oppgaveService.opprettOppgave(any(), any())}
    }
}