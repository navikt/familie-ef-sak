package no.nav.familie.ef.sak.service.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class FerdigstillBehandlingStegTest {

    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskRepository = mockk<TaskRepository>()

    private val task = FerdigstillBehandlingSteg(behandlingService, taskRepository)

    private val fagsak = fagsak()

    @BeforeEach
    internal fun setUp() {
        every { taskRepository.save(any()) } answers { firstArg() }
    }

    @Test
    internal fun `skal opprette publiseringstask og behandlingsstatistikkTask hvis behandlingen er førstegagsbehandling`() {
        task.utførSteg(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING), null)
        verify(exactly = 2) { taskRepository.save(any()) }
    }

    @Test
    internal fun `skal opprette publiseringstask og behandlingsstatistikkTask hvis behandlingen er revurdering`() {
        task.utførSteg(behandling(fagsak, type = BehandlingType.REVURDERING), null)
        verify(exactly = 2) { taskRepository.save(any()) }
    }

    @Test
    internal fun `skal ikke opprette publiseringstask hvis behandlingen er type blankett`() {
        task.utførSteg(behandling(fagsak, type = BehandlingType.BLANKETT), null)
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    internal fun `skal kaste feil hvis behandlingen er av andre typer`() {
        assertThat(catchThrowable { task.utførSteg(behandling(fagsak, type = BehandlingType.KLAGE), null) })
        assertThat(catchThrowable { task.utførSteg(behandling(fagsak, type = BehandlingType.TEKNISK_OPPHØR), null) })
        assertThat(catchThrowable { task.utførSteg(behandling(fagsak, type = BehandlingType.REVURDERING), null) })
    }
}