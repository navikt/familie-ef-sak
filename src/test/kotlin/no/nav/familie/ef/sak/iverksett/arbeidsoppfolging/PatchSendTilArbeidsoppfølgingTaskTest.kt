package no.nav.familie.ef.sak.iverksett.arbeidsoppfolging

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PatchSendTilArbeidsoppfølgingTaskTest : OppslagSpringRunnerTest() {

    val behandlingRepository: BehandlingRepository = mockk()

    val taskService: TaskService = mockk()

    val patchArbeidsoppfølgingController: PatchArbeidsoppfølgingController =
        PatchArbeidsoppfølgingController(behandlingRepository, taskService)

    val taskSlot = slot<List<Task>>()

    @Test
    fun testTriggertidFremITid() {
        val behandlingIds = mutableListOf<UUID>()

        for (i in 0..16000) {
            behandlingIds.add(UUID.randomUUID())
        }

        every { behandlingRepository.finnBehandlingerForPersonerMedAktivStønad(StønadType.OVERGANGSSTØNAD) } returns behandlingIds
        every { taskService.saveAll(capture(taskSlot)) } answers { listOf() }

        patchArbeidsoppfølgingController.patchSendArbeidsoppfølgingInfoForPersonerMedAktivOvergangsstønad(true)

        Assertions.assertThat(taskSlot.captured.size).isEqualTo(behandlingIds.size)
        Assertions.assertThat(taskSlot.captured[600].triggerTid).isAfter(LocalDateTime.now().plusMinutes(9))
        Assertions.assertThat(taskSlot.captured[1200].triggerTid).isAfter(LocalDateTime.now().plusMinutes(19))
        Assertions.assertThat(taskSlot.captured.last().triggerTid).isAfter(LocalDateTime.now().plusMinutes(319))
    }
}
