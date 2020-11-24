package no.nav.familie.ef.sak.no.nav.familie.ef.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.KonsistensavstemmingRepository
import no.nav.familie.ef.sak.repository.domain.Konsistensavstemming
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.task.GrensesnittavstemmingPayload
import no.nav.familie.ef.sak.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.task.KonsistensavstemmingTask
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequest
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import java.time.LocalDate
import java.time.LocalDateTime


internal class KonsistensavstemmingTaskTest {

    private val taskRepository: TaskRepository = mockk()

    private val oppdragClient: OppdragClient = mockk()
    private val konsistensavstemmingRepository: KonsistensavstemmingRepository = mockk()

    private val konsistensavstemmingTask = KonsistensavstemmingTask(oppdragClient, taskRepository, konsistensavstemmingRepository)


    @Test
    fun `doTask skal kalle oppdragClient gitt att datoen for konsistensavstemming er samma som triggerTid`() {
        val konsistensavstemmingRequest = slot<KonsistensavstemmingRequest>()
        every {
            oppdragClient.konsistensavstemming(capture(konsistensavstemmingRequest))
        } returns ArgumentMatchers.anyString()

        every {
            konsistensavstemmingRepository.finnKonsistensavstemmingMedDatoIdag(Stønadstype.OVERGANGSSTØNAD)
        } returns Konsistensavstemming(id = 0, dato = LocalDate.now(), Stønadstype.OVERGANGSSTØNAD)

        konsistensavstemmingTask.doTask(Task(type = "",
                                             payload = payload,
                                             triggerTid = triggerTid))

        assertThat(konsistensavstemmingRequest.captured.avstemmingstidspunkt).isEqualTo(LocalDate.now().atStartOfDay())
        assertThat(konsistensavstemmingRequest.captured.fagsystem).isEqualTo(Stønadstype.OVERGANGSSTØNAD.tilKlassifisering())
    }

    @Test
    fun `onCompletion skal opprette ny konsistensavstemmigTask med morgondagens dato`() {
        val slot = slot<Task>()
        every {
            taskRepository.save(capture(slot))
        } answers {
            slot.captured
        }

        konsistensavstemmingTask.onCompletion(Task(type = KonsistensavstemmingTask.TYPE,
                                                   payload = payload))

        assertThat(slot.captured).isEqualToComparingOnlyGivenFields(Task(
                type = KonsistensavstemmingTask.TYPE,
                payload = payload,
                triggerTid = triggerTid ,
        ),
                                                                    "type",
                                                                    "payload",
                                                                    "triggerTid")

    }

    companion object {
        val triggerTid = LocalDate.now().plusDays(1).atTime(8, 0)
        val payload = objectMapper.writeValueAsString(KonsistensavstemmingPayload(stønadstype = Stønadstype.OVERGANGSSTØNAD, triggerTid = triggerTid))
    }


}