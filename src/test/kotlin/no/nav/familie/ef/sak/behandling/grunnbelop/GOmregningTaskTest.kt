package no.nav.familie.ef.sak.behandling.grunnbelop

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.UUID

internal class GOmregningTaskTest {

    private val taskService = mockk<TaskService>()
    private val gOmregningTask = GOmregningTask(mockk(), taskService, mockk())

    private val taskSlot = slot<Task>()

    private val callId = "123"

    @BeforeEach
    internal fun setUp() {
        MDC.put(MDCConstants.MDC_CALL_ID, callId)
        every { taskService.save(capture(taskSlot)) } answers { firstArg() }
    }

    @AfterEach
    internal fun tearDown() {
        MDC.remove(MDCConstants.MDC_CALL_ID)
    }

    @Test
    internal fun `skal opprette tasks med annet callId enn det som finnes i MDC for å kunne følge hver omregningstask`() {
        every { taskService.finnTaskMedPayloadOgType(any(), any()) } returns null
        gOmregningTask.opprettTask(UUID.randomUUID())

        assertThat(taskSlot.captured.callId).isNotEqualTo(callId)
    }

    @Test
    internal fun `callId er brukt hvis man oppretter vanlig task`() {
        assertThat(Task("", "").callId).isEqualTo(callId)
    }
}