package no.nav.familie.ef.sak.infotrygd.skygge

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Sjekk-og-lagre kjøres i [Propagation.REQUIRES_NEW]: [no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient] kan
 * kalles dypt inne fra store forretningstransaksjoner (migrering, ekstern søknad m.m.), og vi ønsker verken å
 * (a) holde en lås på den unike indeksen like lenge som en slik transaksjon, eller (b) miste en skyggetask fordi
 * en helt urelatert del av en større transaksjon ruller tilbake. Egen transaksjon gir oss dette uavhengig av
 * kallsted, uten at vi må stole på at skyggekjøring bare kalles fra transaksjonsløse ("trygge") deler av koden.
 *
 * Payload inneholder kun operasjon+personIdenter (bevisst, for å holde den unike indeksen liten), så det er dette
 * som regnes som "samme skyggetask" - ikke eksakt request/respons. For å unngå at to podder som skyggekjører
 * samme operasjon+person samtidig faktisk *forsøker* å lagre samme task (og dermed må håndtere en exception fra
 * den unike indeksen, se catch under), forsøkes det først en Postgres advisory-lås ([forsøkLåsForPayloadOgType])
 * nøkkelet på (type, payload). Den er transaksjonsscopet og frigis automatisk ved commit/rollback, og serialiserer
 * kun samtidige kall for samme operasjon+person - andre skyggekjøringer blokkeres ikke.
 */
@Component
class SkyggekjøringTaskLagrer(
    private val taskService: TaskService,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lagreHvisIkkeFinnesFraFør(task: Task) {
        if (!forsøkLåsForPayloadOgType(task.payload, task.type)) {
            logger.info("Skyggetask av type ${task.type} håndteres allerede av et samtidig kall - hopper over")
            return
        }

        val eksisterende = taskService.finnTaskMedPayloadOgType(task.payload, task.type)
        if (eksisterende != null) {
            logger.info("Skyggetask av type ${task.type} med lik payload finnes allerede (id=${eksisterende.id}) - hopper over")
            return
        }
        try {
            taskService.save(task)
        } catch (e: DuplicateKeyException) {
            logger.info("Skyggetask av type ${task.type} ble opprettet samtidig av et annet kall - hopper over")
        } catch (e: DbActionExecutionException) {
            if (e.cause is DuplicateKeyException) {
                logger.info("Skyggetask av type ${task.type} ble opprettet samtidig av et annet kall - hopper over")
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
}
