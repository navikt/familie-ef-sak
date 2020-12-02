package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.api.avstemming.GrensesnittavstemmingDto
import no.nav.familie.ef.sak.api.avstemming.KonsistensavstemmingDto
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.ef.sak.task.GrensesnittavstemmingPayload
import no.nav.familie.ef.sak.task.GrensesnittavstemmingTask
import no.nav.familie.ef.sak.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.task.KonsistensavstemmingTask
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AvstemmingServiceTest {

    private val oppdragClient: OppdragClient = mockk()
    private val taskRepository: TaskRepository = mockk()
    private val tilkjentYtelseRepository: TilkjentYtelseRepository = mockk()

    private val avstemmingService: AvstemmingService =
            AvstemmingService(oppdragClient, taskRepository, tilkjentYtelseRepository)


    @Test
    fun `opprettKonsistenavstemmingTask skal kalle taskRepository med task som argument`() {
        val datoForAvstemming = LocalDate.of(2020, 12, 24)

        val taskIterable = slot<Iterable<Task>>()
        every {
            taskRepository.saveAll(capture(taskIterable))
        } answers {
            taskIterable.captured
        }

        every {
            tilkjentYtelseRepository.finnAktiveBehandlinger(any(), any())
        } answers {
            emptyList()
        }

        avstemmingService.opprettKonsistenavstemmingTasker(listOf(KonsistensavstemmingDto(datoForAvstemming,
                                                                                   stønadstype = Stønadstype.OVERGANGSSTØNAD)))

        val payload = KonsistensavstemmingPayload(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                                  triggerTid = datoForAvstemming.atTime(8, 0))

        val capturedTask = taskIterable.captured.iterator().next()
        assertThat(capturedTask).isEqualToComparingOnlyGivenFields(Task(type = KonsistensavstemmingTask.TYPE,
                                                                        payload = objectMapper.writeValueAsString(payload),

                                                                        triggerTid = datoForAvstemming.atTime(8, 0)),
                                                                   "type",
                                                                   "payload",
                                                                   "triggerTid")

    }

    @Test
    fun `opprettGrensesnittavstemmingTask skal kalle taskRepository med task som argument`() {
        val taskSlot = slot<Task>()
        every {
            taskRepository.save(capture(taskSlot))
        } answers {
            taskSlot.captured
        }

        avstemmingService.opprettGrensesnittavstemmingTask(GrensesnittavstemmingDto(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                                                                    fraDato = LocalDate.of(2020, 11, 24)))

        val payload = GrensesnittavstemmingPayload(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                                   fraDato = LocalDate.of(2020, 11, 24))
        assertThat(taskSlot.captured).isEqualToComparingOnlyGivenFields(Task(type = GrensesnittavstemmingTask.TYPE,
                                                                             payload = objectMapper.writeValueAsString(payload),
                                                                             triggerTid = LocalDate.of(2020, 11, 25)
                                                                                     .atTime(8, 0)),
                                                                        "type",
                                                                        "payload",
                                                                        "triggerTid")
    }

}