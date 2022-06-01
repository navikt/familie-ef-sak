package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VentePåStatusFraIverksettTest {

    private val iverksettClient = mockk<IverksettClient>()
    private val taskRepository = mockk<TaskRepository>()

    private val steg = VentePåStatusFraIverksett(iverksettClient, taskRepository)

    @BeforeEach
    internal fun setUp() {
        every { taskRepository.save(any()) } answers { firstArg() }
    }

    @Test
    internal fun `KORRIGERING_UTEN_BREV - ok når status er OK_MOT_OPPDRAG`() {
        mockIverksettStatus(IverksettStatus.OK_MOT_OPPDRAG)

        steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.KORRIGERING_UTEN_BREV), null)

        verifyHarOpprettetTask()
    }

    @Test
    internal fun `migrering - ok når status er OK_MOT_OPPDRAG`() {
        mockIverksettStatus(IverksettStatus.OK_MOT_OPPDRAG)

        steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.MIGRERING), null)

        verifyHarOpprettetTask()
    }

    @Test
    internal fun `g-omregning - ok når status er OK_MOT_OPPDRAG`() {
        mockIverksettStatus(IverksettStatus.OK_MOT_OPPDRAG)

        steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.G_OMREGNING), null)

        verifyHarOpprettetTask()
    }

    @Test
    internal fun `migrering - IKKE_PÅBEGYNT kaster feil`() {
        mockIverksettStatus(IverksettStatus.IKKE_PÅBEGYNT)

        assertThatThrownBy { steg.utførSteg(saksbehandling(årsak = BehandlingÅrsak.MIGRERING), null) }
            .isInstanceOf(TaskExceptionUtenStackTrace::class.java)

        verifyHarIkkeOpprettetTask()
    }

    private fun verifyHarIkkeOpprettetTask() {
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    private fun verifyHarOpprettetTask() {
        verify(exactly = 1) { taskRepository.save(any()) }
    }

    private fun mockIverksettStatus(status: IverksettStatus) {
        every { iverksettClient.hentStatus(any()) } returns status
    }
}
