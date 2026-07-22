package no.nav.familie.ef.sak.infotrygd.skygge

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbAction
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.jdbc.core.JdbcTemplate

class SkyggekjøringTaskLagrerTest {
    private val taskService = mockk<TaskService>()
    private val jdbcTemplate = mockk<JdbcTemplate>(relaxed = true)
    private val skyggekjøringTaskLagrer = SkyggekjøringTaskLagrer(taskService, jdbcTemplate)

    private val task = Task(type = "skyggekjørInfotrygd", payload = "{\"foo\":\"bar\"}", properties = java.util.Properties())

    @Test
    fun `lagrer ikke ny task dersom en identisk task allerede finnes`() {
        every { taskService.finnTaskMedPayloadOgType(task.payload, task.type) } returns mockk<Task>(relaxed = true)

        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(task)

        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `tar advisory-lås nøkkelet på type og payload før sjekk mot databasen`() {
        every { taskService.finnTaskMedPayloadOgType(task.payload, task.type) } returns null
        every { taskService.save(task) } returns task

        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(task)

        val forventetNøkkel = "${task.type}|${task.payload}".hashCode()
        verifyOrder {
            jdbcTemplate.execute("SELECT pg_advisory_xact_lock($forventetNøkkel)")
            taskService.finnTaskMedPayloadOgType(task.payload, task.type)
            taskService.save(task)
        }
    }

    @Test
    fun `lagrer ny task dersom ingen identisk task finnes fra før`() {
        every { taskService.finnTaskMedPayloadOgType(task.payload, task.type) } returns null
        every { taskService.save(task) } returns task

        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(task)

        verify(exactly = 1) { taskService.save(task) }
    }

    @Test
    fun `svelger DuplicateKeyException dersom en annen tråd rekker å lagre samme task først`() {
        every { taskService.finnTaskMedPayloadOgType(task.payload, task.type) } returns null
        every { taskService.save(task) } throws DuplicateKeyException("task_payload_type_idx")

        assertThatCode { skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(task) }.doesNotThrowAnyException()
    }

    @Test
    fun `svelger DbActionExecutionException som skyldes DuplicateKeyException`() {
        every { taskService.finnTaskMedPayloadOgType(task.payload, task.type) } returns null
        every { taskService.save(task) } throws
            DbActionExecutionException(mockk<DbAction<*>>(relaxed = true), DuplicateKeyException("task_payload_type_idx"))

        assertThatCode { skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(task) }.doesNotThrowAnyException()
    }

    @Test
    fun `kaster videre DbActionExecutionException som ikke skyldes DuplicateKeyException`() {
        every { taskService.finnTaskMedPayloadOgType(task.payload, task.type) } returns null
        val exception = DbActionExecutionException(mockk<DbAction<*>>(relaxed = true), RuntimeException("noe annet gikk galt"))
        every { taskService.save(task) } throws exception

        assertThatCode { skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(task) }
            .isInstanceOf(DbActionExecutionException::class.java)
    }
}
