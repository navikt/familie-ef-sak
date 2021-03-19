package no.nav.familie.ef.sak.task

import io.mockk.*
import no.nav.familie.ef.sak.api.avstemming.GrensesnittavstemmingDto
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import java.time.LocalDate
import java.time.LocalDateTime

internal class GrensesnittavstemmingTaskTest {

    private val avstemmingService: AvstemmingService = mockk()

    private val grensesnittavstemmingTask: GrensesnittavstemmingTask =
            GrensesnittavstemmingTask(avstemmingService)

    @Test
    fun `doTask skal kalle oppdragClient med fradato fra payload og dato for triggerTid som parametere`() {
        val fradatoSlot = slot<LocalDateTime>()
        val tildatoSlot = slot<LocalDateTime>()
        val stønadstypeSlot = slot<Stønadstype>()
        justRun {
            avstemmingService.grensesnittavstemOppdrag(capture(fradatoSlot), capture(tildatoSlot), capture(stønadstypeSlot))
        }

        grensesnittavstemmingTask.doTask(Task(type = "",
                                              payload = payload,
                                              triggerTid = LocalDateTime.of(2018, 4, 19, 8, 0)))

        assertThat(fradatoSlot.captured).isEqualTo(LocalDate.of(2018, 4, 18).atStartOfDay())
        assertThat(tildatoSlot.captured).isEqualTo(LocalDate.of(2018, 4, 19).atStartOfDay())
        assertThat(stønadstypeSlot.captured).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
    }

    @Test
    fun `onCompletion skal opprette ny grensesnittavstemmingTask med dato for forrige triggerTid som payload`() {
        val triggeTid = LocalDateTime.of(2018, 4, 18, 8, 0)
        val slot = slot<GrensesnittavstemmingDto>()
        every {
            avstemmingService.opprettGrensesnittavstemmingTask(capture(slot))
        } returns mockk()

        grensesnittavstemmingTask.onCompletion(Task(type = GrensesnittavstemmingTask.TYPE,
                                                    payload = payload,
                                                    triggerTid = triggeTid))

        assertThat(slot.captured).isEqualTo(GrensesnittavstemmingDto(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                                                     fraDato = triggeTid.toLocalDate()))
    }

    companion object {

        val payload = objectMapper.writeValueAsString(GrensesnittavstemmingPayload(fraDato = LocalDate.of(2018, 4, 18),
                                                                                   stønadstype = Stønadstype.OVERGANGSSTØNAD))
    }


}

