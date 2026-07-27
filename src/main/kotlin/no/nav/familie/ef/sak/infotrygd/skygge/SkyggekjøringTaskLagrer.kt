package no.nav.familie.ef.sak.infotrygd.skygge

import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskType
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Sjekk-og-lagre kjøres i [Propagation.REQUIRES_NEW]: [no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient] kan
 * kalles dypt inne fra store forretningstransaksjoner (migrering, ekstern søknad m.m.), og vi ønsker verken å
 * (a) holde en lås på den unike indeksen like lenge som en slik transaksjon, eller (b) miste en skyggetask fordi
 * en helt urelatert del av en større transaksjon ruller tilbake. Egen transaksjon gir oss dette uavhengig av
 * kallsted, uten at vi må stole på at skyggekjøring bare kalles fra transaksjonsløse ("trygge") deler av koden.
 */
@Component
class SkyggekjøringTaskLagrer(
    private val taskProdusent: TaskProdusent,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <P : Any> lagreHvisIkkeFinnesFraFør(
        type: TaskType<P>,
        payload: P,
        metadata: Map<String, String> = emptyMap(),
    ) {
        val serialisertPayload = type.serialiser(payload)

        if (!forsøkLåsForPayloadOgType(serialisertPayload, type.navn)) {
            logger.info("Skyggetask av type ${type.navn} håndteres allerede av et samtidig kall - hopper over")
            return
        }

        if (finnesFraFør(type.navn, serialisertPayload)) {
            logger.info("Skyggetask av type ${type.navn} med lik payload finnes allerede fra før - hopper over")
            return
        }

        try {
            taskProdusent.opprettIEgenTransaksjon(type, payload, Instant.now(), metadata)
        } catch (e: PSQLException) {
            if (e.sqlState == PSQLState.UNIQUE_VIOLATION.state) {
                logger.info("Skyggetask av type ${type.navn} ble opprettet samtidig av et annet kall - hopper over")
            } else {
                throw e
            }
        }
    }

    private fun finnesFraFør(
        type: String,
        payload: String,
    ): Boolean {
        val antall =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prosessering.task WHERE type = ? AND payload = ?",
                Int::class.java,
                type,
                payload,
            ) ?: 0
        return antall > 0
    }

    private fun forsøkLåsForPayloadOgType(
        payload: String,
        type: String,
    ): Boolean {
        val låsnøkkel = "$type|$payload".hashCode()
        return jdbcTemplate.queryForObject("SELECT pg_try_advisory_xact_lock($låsnøkkel)", Boolean::class.java) ?: false
    }
}
