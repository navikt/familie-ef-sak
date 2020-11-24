package no.nav.familie.ef.sak.task

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import java.time.LocalDate
import java.time.LocalDateTime

internal class GrensesnittavstemmingTaskTest {

    private val taskRepository: TaskRepository = mockk()

    private val oppdragClient: OppdragClient = mockk()

    private val grensesnittavstemmingTask: GrensesnittavstemmingTask =
            GrensesnittavstemmingTask(oppdragClient, taskRepository)

    @Test
    fun `doTask skal kalle oppdragClient med fradato fra payload og dato for triggerTid som parametere`() {
        val grensesnittavstemmingRequest = slot<GrensesnittavstemmingRequest>()
        every {
            oppdragClient.grensesnittavstemming(capture(grensesnittavstemmingRequest))
        } returns "Dont care string"

        grensesnittavstemmingTask.doTask(Task(type = "",
                                              payload = payload,
                                              triggerTid = LocalDateTime.of(2018, 4, 19, 8, 0)))

        assertThat(grensesnittavstemmingRequest.captured.fra).isEqualTo(LocalDate.of(2018, 4, 18).atStartOfDay())
        assertThat(grensesnittavstemmingRequest.captured.til).isEqualTo(LocalDate.of(2018, 4, 19).atStartOfDay())
        assertThat(grensesnittavstemmingRequest.captured.fagsystem).isEqualTo(Stønadstype.OVERGANGSSTØNAD.tilKlassifisering())
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
                                                    payload = payload,
                                                    triggerTid = LocalDateTime.of(2018, 4, 18, 8, 0)))

        assertThat(slot.captured).isEqualToComparingOnlyGivenFields(Task(type = GrensesnittavstemmingTask.TYPE,
                                                                         payload = payload,
                                                                         triggerTid = LocalDateTime.of(2018, 4, 19, 8, 0)),
                                                                    "type",
                                                                    "payload",
                                                                    "triggerTid")
    }

    companion object {

        val payload = objectMapper.writeValueAsString(GrensesnittavstemmingPayload(fraDato = LocalDate.of(2018, 4, 18),
                                                                                   stønadstype = Stønadstype.OVERGANGSSTØNAD))
    }


}

