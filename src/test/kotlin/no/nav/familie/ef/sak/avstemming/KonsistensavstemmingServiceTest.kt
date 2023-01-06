package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.OptimisticLockingFailureException
import java.time.LocalDate

internal class KonsistensavstemmingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var repository: KonsistensavstemmingJobbRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var konsistensavstemmingService: KonsistensavstemmingService

    @BeforeEach
    internal fun setUp() {
        repository.deleteAll() // sletter jobs fra db migrering
    }

    @Test
    internal fun `skal kaste optimistic lock`() {
        val jobb = repository.insert(KonsistensavstemmingJobb(triggerdato = LocalDate.now()))
        repository.update(jobb)
        assertThat(catchThrowable { repository.update(jobb) })
            .isInstanceOf(OptimisticLockingFailureException::class.java)
    }

    @Test
    internal fun `skal ikke oppdatere jobb som er satt til tre dager frem i tid`() {
        repository.insert(KonsistensavstemmingJobb(triggerdato = LocalDate.now().plusDays(3)))

        konsistensavstemmingService.opprettTasks()

        assertThat(taskService.findAll()).isEmpty()
    }

    @Test
    internal fun `skal oppdatere jobb som er satt til en dag fram i tid`() {
        val nesteJobb = LocalDate.now().plusDays(1)
        val jobb = repository.insert(KonsistensavstemmingJobb(triggerdato = nesteJobb))

        konsistensavstemmingService.opprettTasks()

        val oppdatertJobb = repository.findByIdOrThrow(jobb.id)
        assertThat(oppdatertJobb.opprettet).isTrue
        val tasks = taskService.findAll().toList().sortedBy { it.triggerTid }
        assertThat(tasks).hasSize(2)
        assertThat(tasks[0].triggerTid).isEqualTo(nesteJobb.atTime(22, 0))
        assertThat(tasks[1].triggerTid).isEqualTo(nesteJobb.atTime(22, 20))
    }
}
