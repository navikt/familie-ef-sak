package no.nav.familie.ef.sak.infotrygd.skygge

import efterlatte.prosessering.spring.TaskService
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException

/**
 * Sjekk-og-lagre kjøres i [Propagation.REQUIRES_NEW]: [no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient] kan
 * kalles dypt inne fra store forretningstransaksjoner (migrering, ekstern søknad m.m.), og vi ønsker verken å
 * (a) holde en lås på den unike indeksen like lenge som en slik transaksjon, eller (b) miste en skyggetask fordi
 * en helt urelatert del av en større transaksjon ruller tilbake. Egen transaksjon gir oss dette uavhengig av
 * kallsted, uten at vi må stole på at skyggekjøring bare kalles fra transaksjonsløse ("trygge") deler av koden.
 *
 * For å unngå at to podder som skyggekjører nøyaktig samme kall samtidig faktisk *forsøker* å lagre samme task
 * (og dermed må håndtere en exception fra den unike indeksen, se catch under), forsøkes det først en Postgres
 * advisory-lås ([forsøkLåsForPayloadOgType]) nøkkelet på (type, payload). Den er transaksjonsscopet og frigis
 * automatisk ved commit/rollback, og serialiserer kun samtidige kall for *nøyaktig* samme skyggetask - andre
 * skyggekjøringer blokkeres ikke.
 *
 * [efterlatte.prosessering.spring.TaskService.opprett] går rett på JDBC (se [efterlatte.prosessering.postgres.PostgresTaskRepository]),
 * uten Springs vanlige exception-oversettelse - et brudd på den unike indeksen (idx_prosessering_task_type_payload,
 * se V170__efterlatte_prosessering_task.sql) dukker derfor opp som en rå [SQLException] med SQLSTATE 23505, ikke
 * Springs [org.springframework.dao.DuplicateKeyException] slik det gamle no.nav.familie.prosessering-biblioteket ga.
 */
@Component
class SkyggekjøringTaskLagrer(
    private val taskService: TaskService,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lagreHvisIkkeFinnesFraFør(payload: SkyggeInfotrygdPayload) {
        val type = SkyggekjørInfotrygdTask.TYPE
        val serialisertPayload = type.serialiser(payload)

        if (!forsøkLåsForPayloadOgType(serialisertPayload, type.navn)) {
            logger.info("Skyggetask av type ${type.navn} håndteres allerede av et samtidig kall - hopper over")
            return
        }

        try {
            taskService.opprett(type = type, payload = payload)
        } catch (e: SQLException) {
            if (e.sqlState == UNIK_INDEKS_SQLSTATE) {
                logger.info("Skyggetask av type ${type.navn} med lik payload finnes allerede - hopper over")
            } else {
                throw e
            }
        }
    }

    private fun forsøkLåsForPayloadOgType(
        payload: String,
        type: String,
    ): Boolean {
        val låsnøkkel = "$type|$payload".hashCode()
        return jdbcTemplate.queryForObject("SELECT pg_try_advisory_xact_lock($låsnøkkel)", Boolean::class.java) ?: false
    }

    companion object {
        private const val UNIK_INDEKS_SQLSTATE = "23505"
    }
}
