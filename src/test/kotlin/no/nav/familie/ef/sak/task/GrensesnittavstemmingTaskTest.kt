package no.nav.familie.ef.sak.task

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class GrensesnittavstemmingTaskTest {

    private val taskRepository: TaskRepository = mockk()

    private val avstemmingService: AvstemmingService = mockk()

    private val grensesnittavstemmingTask: GrensesnittavstemmingTask =
            GrensesnittavstemmingTask(avstemmingService, taskRepository)

    @Test
    fun `doTask skal kalle avstemmingService med fradato fra payload og dato for triggerTid som parametere`() {
        val fradatoSlot = slot<LocalDate>()
        val tildatoSlot = slot<LocalDate>()
        justRun {
            avstemmingService.grensesnittavstemOppdrag(capture(fradatoSlot), capture(tildatoSlot))
        }

        grensesnittavstemmingTask.doTask(Task(type = "",
                                              payload = LocalDate.of(2018, 4, 18).toString(),
                                              triggerTid = LocalDateTime.of(2018, 4, 19, 8, 0)))

        assertThat(fradatoSlot.captured).isEqualTo(LocalDate.of(2018, 4, 18))
        assertThat(tildatoSlot.captured).isEqualTo(LocalDate.of(2018, 4, 19))
    }

    @Test
    fun `onCompletion skal opprette ny grensesnittavstemmingTask med dato for forrige triggerTid som payload`() {
        val slot = slot<Task>()
        every {
            taskRepository.save(capture(slot))
        } answers {
            slot.captured
        }

        grensesnittavstemmingTask.onCompletion(Task(type = GrensesnittavstemmingTask.TYPE,
                                                    payload = "",
                                                    triggerTid = LocalDateTime.of(2018, 4, 18, 8, 0)))

        assertThat(slot.captured).isEqualToComparingOnlyGivenFields(Task(type = GrensesnittavstemmingTask.TYPE,
                                                                         payload = LocalDate.of(2018, 4, 18).toString(),
                                                                         triggerTid = LocalDateTime.of(2018, 4, 19, 8, 0)),
                                                                    "type",
                                                                    "payload",
                                                                    "triggerTid")
    }


}

