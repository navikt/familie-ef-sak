package no.nav.familie.ef.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OpprettOppgaveForOpprettetBehandlingTaskTest {

    val behandlingService = mockk<BehandlingService>()
    val oppgaveService = mockk<OppgaveService>()
    val taskService = mockk<TaskService>()
    val opprettOppgaveForOpprettetBehandlingTask =
        OpprettOppgaveForOpprettetBehandlingTask(behandlingService, oppgaveService, taskService)

    val oppgaveId = 1L

    val opprettTaskSlot = slot<Task>()

    @BeforeEach
    internal fun setUp() {
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns oppgaveId
        every { taskService.save(capture(opprettTaskSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `Skal opprette oppgave hvis behandlingen har status opprettet eller utredes`() {
        listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES).forEach { behandlingStatus ->
            val behandling = mockBehandling(behandlingStatus)

            opprettOppgaveForOpprettetBehandlingTask.doTask(
                OpprettOppgaveForOpprettetBehandlingTask.opprettTask(OpprettOppgaveTaskData(behandling.id, ""))
            )

            val opprettetTaskData =
                objectMapper.readValue<BehandlingsstatistikkTaskPayload>(opprettTaskSlot.captured.payload)
            assertThat(opprettetTaskData.oppgaveId).isEqualTo(oppgaveId)
            verify(exactly = 1) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) }
        }
    }

    @Test
    internal fun `Skal ikke opprette oppgave hvis behandlingen ikke har status opprettet eller utredes`() {
        BehandlingStatus.values().filterNot { it == BehandlingStatus.OPPRETTET || it == BehandlingStatus.UTREDES }
            .forEach { behandlingStatus ->
                val behandling = mockBehandling(behandlingStatus)

                opprettOppgaveForOpprettetBehandlingTask.doTask(
                    OpprettOppgaveForOpprettetBehandlingTask.opprettTask(OpprettOppgaveTaskData(behandling.id, ""))
                )

                val opprettetTaskData =
                    objectMapper.readValue<BehandlingsstatistikkTaskPayload>(opprettTaskSlot.captured.payload)
                assertThat(opprettetTaskData.oppgaveId).isEqualTo(oppgaveId)
                verify(exactly = 1) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) }
            }
    }

    private fun mockBehandling(status: BehandlingStatus): Behandling {
        val behandling = behandling(status = status)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        return behandling
    }
}