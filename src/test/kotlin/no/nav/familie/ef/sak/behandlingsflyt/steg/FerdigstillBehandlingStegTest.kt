package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FerdigstillBehandlingStegTest {

    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskService = mockk<TaskService>()

    private val task = FerdigstillBehandlingSteg(behandlingService, taskService)

    private val fagsak = fagsak()
    private val taskSlot = mutableListOf<Task>()

    @BeforeEach
    internal fun setUp() {
        taskSlot.clear()
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `skal opprette publiseringstask og behandlingsstatistikkTask hvis behandlingen er førstegagsbehandling`() {
        task.utførSteg(saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING)), null)
        verify(exactly = 2) { taskService.save(any()) }
    }

    @Test
    internal fun `skal opprette publiseringstask og behandlingsstatistikkTask hvis behandlingen er revurdering`() {
        task.utførSteg(saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.REVURDERING)), null)
        verify(exactly = 2) { taskService.save(any()) }
        assertThat(taskSlot).hasSize(2)
        assertThat(taskSlot.single { it.type == BehandlingsstatistikkTask.TYPE })
    }

    @Test
    internal fun `skal ikke opprette BehandlingsstatistikkTask for maskinelle g-omregninger`() {
        utførStegGOmregning(SYSTEM_FORKORTELSE)
        assertThat(taskSlot).hasSize(1)
        assertThat(taskSlot.none { it.type == BehandlingsstatistikkTask.TYPE })
    }

    @Test
    internal fun `skal opprette BehandlingsstatistikkTask for manuelle g-omregninger`() {
        utførStegGOmregning("z123456")
        assertThat(taskSlot).hasSize(2)
        assertThat(taskSlot.single { it.type == BehandlingsstatistikkTask.TYPE })
    }

    @Test
    internal fun `skal kaste feil hvis behandlingen er av andre typer`() {
        assertThat(
            catchThrowable {
                task.utførSteg(
                    saksbehandling(fagsak, behandling(fagsak, type = BehandlingType.REVURDERING)),
                    null
                )
            }
        )
    }

    private fun utførStegGOmregning(opprettetAv: String) {
        task.utførSteg(
            saksbehandling(
                fagsak,
                behandling(
                    fagsak,
                    type = BehandlingType.REVURDERING,
                    årsak = BehandlingÅrsak.G_OMREGNING
                )
            ).copy(opprettetAv = opprettetAv),
            null
        )
    }
}
