package no.nav.familie.ef.sak.infotrygd.skygge

import efterlatte.prosessering.TaskId
import efterlatte.prosessering.spring.TaskService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import org.springframework.jdbc.core.JdbcTemplate

class SkyggekjøringTaskLagrerTest {
    private val taskService = mockk<TaskService>()
    private val jdbcTemplate = mockk<JdbcTemplate>()
    private val skyggekjøringTaskLagrer = SkyggekjøringTaskLagrer(taskService, jdbcTemplate)

    private val payload =
        SkyggeInfotrygdPayload(
            operasjon = SkyggeInfotrygdOperasjon.HENT_PERIODER,
            request = "{\"foo\":\"bar\"}",
            forventetRespons = "{}",
        )
    private val type = SkyggekjørInfotrygdTask.TYPE
    private val serialisertPayload = type.serialiser(payload)
    private val forventetLåsnøkkel = "${type.navn}|$serialisertPayload".hashCode()
    private val forventetLåsSql = "SELECT pg_try_advisory_xact_lock($forventetLåsnøkkel)"

    @BeforeEach
    fun setup() {
        // Låsen tas som hovedregel - egne tester overstyrer dette for å simulere at et annet samtidig kall alt holder den.
        every { jdbcTemplate.queryForObject(forventetLåsSql, Boolean::class.java) } returns true
        every { taskService.opprett(type, payload) } returns TaskId(1L)
    }

    @Test
    fun `lagrer ikke ny task dersom advisory-låsen for type og payload allerede holdes av et samtidig kall`() {
        every { jdbcTemplate.queryForObject(forventetLåsSql, Boolean::class.java) } returns false

        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(payload)

        verify(exactly = 0) { taskService.opprett(any(), any<SkyggeInfotrygdPayload>()) }
    }

    @Test
    fun `forsøker advisory-lås nøkkelet på type og payload før tasken opprettes`() {
        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(payload)

        verifyOrder {
            jdbcTemplate.queryForObject(forventetLåsSql, Boolean::class.java)
            taskService.opprett(type, payload)
        }
    }

    @Test
    fun `lagrer ny task dersom ingen identisk task finnes fra før`() {
        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(payload)

        verify(exactly = 1) { taskService.opprett(type, payload) }
    }

    @Test
    fun `svelger SQLException for unik-indeks-brudd dersom en annen tråd rekker å lagre samme task først`() {
        every { taskService.opprett(type, payload) } throws PSQLException("duplikat", PSQLState.UNIQUE_VIOLATION)

        assertThatCode { skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(payload) }.doesNotThrowAnyException()
    }

    @Test
    fun `kaster videre SQLException som ikke skyldes unik-indeks-brudd`() {
        val exception = PSQLException("noe annet gikk galt", PSQLState.CONNECTION_FAILURE)
        every { taskService.opprett(type, payload) } throws exception

        assertThatCode { skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(payload) }
            .isInstanceOf(java.sql.SQLException::class.java)
    }
}
