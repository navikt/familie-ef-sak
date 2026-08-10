package no.nav.familie.ef.sak.infotrygd.skygge

import efterlatte.prosessering.TaskId
import efterlatte.prosessering.TaskProdusent
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
    private val taskProdusent = mockk<TaskProdusent>()
    private val jdbcTemplate = mockk<JdbcTemplate>()
    private val skyggekjøringTaskLagrer = SkyggekjøringTaskLagrer(taskProdusent, jdbcTemplate)

    private val type = SkyggekjørInfotrygdTask.TYPE
    private val payload =
        SkyggekjørInfotrygdTask.opprettPayload(
            operasjon = SkyggeInfotrygdOperasjon.HENT_PERIODER,
            personIdenter = setOf("12345678910"),
        )
    private val metadata = SkyggekjørInfotrygdTask.opprettMetadata(request = "{\"foo\":\"bar\"}", forventetRespons = "{}")

    private val serialisertPayload = type.serialiser(payload)
    private val forventetLåsnøkkel = "${type.navn}|$serialisertPayload".hashCode()
    private val forventetLåsSql = "SELECT pg_try_advisory_xact_lock($forventetLåsnøkkel)"
    private val forventetFinnesFraFørSql = "SELECT COUNT(*) FROM prosessering_task WHERE type = ? AND payload = ?"

    @BeforeEach
    fun setup() {
        // Låsen tas, og ingen identisk task finnes fra før, som hovedregel - egne tester overstyrer dette for å
        // simulere at et annet samtidig kall alt holder låsen, eller at en identisk task allerede finnes.
        every { jdbcTemplate.queryForObject(forventetLåsSql, Boolean::class.java) } returns true
        every {
            jdbcTemplate.queryForObject(forventetFinnesFraFørSql, Int::class.java, type.navn, serialisertPayload)
        } returns 0
        every { taskProdusent.opprettIEgenTransaksjon(type, payload, any(), metadata) } returns TaskId(1L)
    }

    @Test
    fun `lagrer ikke ny task dersom advisory-låsen for type og payload allerede holdes av et samtidig kall`() {
        every { jdbcTemplate.queryForObject(forventetLåsSql, Boolean::class.java) } returns false

        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(type, payload, metadata)

        verify(exactly = 0) { taskProdusent.opprettIEgenTransaksjon(type, payload, any(), metadata) }
    }

    @Test
    fun `lagrer ikke ny task dersom en identisk task allerede finnes fra før`() {
        every {
            jdbcTemplate.queryForObject(forventetFinnesFraFørSql, Int::class.java, type.navn, serialisertPayload)
        } returns 1

        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(type, payload, metadata)

        verify(exactly = 0) { taskProdusent.opprettIEgenTransaksjon(type, payload, any(), metadata) }
    }

    @Test
    fun `forsøker advisory-lås og sjekker om identisk task finnes fra før, i den rekkefølgen, før tasken opprettes`() {
        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(type, payload, metadata)

        verifyOrder {
            jdbcTemplate.queryForObject(forventetLåsSql, Boolean::class.java)
            jdbcTemplate.queryForObject(forventetFinnesFraFørSql, Int::class.java, type.navn, serialisertPayload)
            taskProdusent.opprettIEgenTransaksjon(type, payload, any(), metadata)
        }
    }

    @Test
    fun `lagrer ny task med request og forventet respons som metadata dersom ingen identisk task finnes fra før`() {
        skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(type, payload, metadata)

        verify(exactly = 1) { taskProdusent.opprettIEgenTransaksjon(type, payload, any(), metadata) }
    }

    @Test
    fun `svelger PSQLException for unik-indeks-brudd dersom en annen tråd rekker å lagre samme task først`() {
        every {
            taskProdusent.opprettIEgenTransaksjon(type, payload, any(), metadata)
        } throws PSQLException("duplikat", PSQLState.UNIQUE_VIOLATION)

        assertThatCode { skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(type, payload, metadata) }.doesNotThrowAnyException()
    }

    @Test
    fun `kaster videre PSQLException som ikke skyldes unik-indeks-brudd`() {
        val exception = PSQLException("noe annet gikk galt", PSQLState.CONNECTION_FAILURE)
        every { taskProdusent.opprettIEgenTransaksjon(type, payload, any(), metadata) } throws exception

        assertThatCode { skyggekjøringTaskLagrer.lagreHvisIkkeFinnesFraFør(type, payload, metadata) }
            .isInstanceOf(PSQLException::class.java)
    }
}
