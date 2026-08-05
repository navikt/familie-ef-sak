package no.nav.familie.ef.sak.infrastruktur.prosessering

import efterlatte.prosessering.Metadata
import efterlatte.prosessering.Status
import efterlatte.prosessering.Stoppaarsak
import efterlatte.prosessering.Task
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class EfterlatteProsesseringAdminRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentTasks(
        status: Status?,
        type: String?,
        antall: Int,
    ): List<Task> {
        val betingelser = mutableListOf<String>()
        val parametre = mutableListOf<Any>()

        if (status != null) {
            betingelser.add("status = ?")
            parametre.add(status.name)
        }
        if (type != null) {
            betingelser.add("type = ?")
            parametre.add(type)
        }
        parametre.add(antall)

        val where = if (betingelser.isEmpty()) "" else betingelser.joinToString(prefix = " WHERE ", separator = " AND ")
        val sql = "SELECT * FROM $TABELL$where ORDER BY opprettet_tid DESC, id DESC LIMIT ?"

        return jdbcTemplate.query(sql, taskRowMapper, *parametre.toTypedArray())
    }

    fun hentTask(id: Long): Task? =
        jdbcTemplate
            .query("SELECT * FROM $TABELL WHERE id = ?", taskRowMapper, id)
            .firstOrNull()

    fun hentAntallPerStatus(): Map<Status, Int> =
        jdbcTemplate
            .query("SELECT status, COUNT(*) AS antall FROM $TABELL GROUP BY status") { resultSet, _ ->
                Status.valueOf(resultSet.getString("status")) to resultSet.getInt("antall")
            }.toMap()

    fun hentTyper(): List<String> = jdbcTemplate.query("SELECT DISTINCT type FROM $TABELL ORDER BY type") { resultSet, _ -> resultSet.getString("type") }

    /**
     * Setter en stoppet eller avbrutt task tilbake til KLAR og nullstiller feiltelleren, slik at
     * motoren får fulle retries på nytt. Returnerer `false` hvis tasken ikke er i en tilstand som
     * kan rekjøres.
     */
    fun rekjør(id: Long): Boolean = jdbcTemplate.update(REKJØR_SQL, id) > 0

    /** Avfeier en task som aldri skal kjøres. Returnerer `false` hvis tasken ikke kan avbrytes. */
    fun avbryt(id: Long): Boolean = jdbcTemplate.update(AVBRYT_SQL, id) > 0

    private val taskRowMapper = RowMapper { resultSet, _ -> resultSet.tilTask() }

    private fun ResultSet.tilTask(): Task =
        Task(
            id = getLong("id"),
            type = getString("type"),
            status = Status.valueOf(getString("status")),
            payload = getString("payload"),
            triggerTid = getTimestamp("trigger_tid").toInstant(),
            opprettetTid = getTimestamp("opprettet_tid").toInstant(),
            plukketTid = getTimestamp("plukket_tid")?.toInstant(),
            antallFeil = getInt("antall_feil"),
            stoppaarsak = getString("stoppaarsak")?.let { Stoppaarsak.valueOf(it) },
            versjon = getLong("versjon"),
            metadata = Metadata.deserialiser(getString("metadata")),
        )

    companion object {
        private const val TABELL = "prosessering_task"

        private val REKJØR_SQL =
            """
            UPDATE $TABELL
               SET status = 'KLAR',
                   stoppaarsak = NULL,
                   antall_feil = 0,
                   plukket_tid = NULL,
                   trigger_tid = now(),
                   versjon = versjon + 1
             WHERE id = ? AND status IN ('STOPPET', 'AVBRUTT')
            """.trimIndent()

        private val AVBRYT_SQL =
            """
            UPDATE $TABELL
               SET status = 'AVBRUTT',
                   plukket_tid = NULL,
                   versjon = versjon + 1
             WHERE id = ? AND status IN ('KLAR', 'STOPPET')
            """.trimIndent()
    }
}
