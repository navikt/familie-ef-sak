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
 * For å unngå at to podder som skyggekjører nøyaktig samme kall samtidig faktisk *forsøker* å lagre samme task
 * (og dermed må håndtere en exception fra den unike indeksen, se catch under), tas det først en Postgres
 * advisory-lås ([laasForPayloadOgType]) nøkkelet på (type, payload). Den er transaksjonsscopet og frigis
 * automatisk ved commit/rollback, og serialiserer kun samtidige kall for *nøyaktig* samme skyggetask - andre
 * skyggekjøringer blokkeres ikke. Den første som får låsen gjør sjekk+lagre+commit; de påfølgende ser deretter
 * allerede den lagrede tasken i sjekken og hopper over lagring i stedet for å forsøke et duplikat-insert.
 */
@Component
class SkyggekjøringTaskLagrer(
    private val taskService: TaskService,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lagreHvisIkkeFinnesFraFør(task: Task) {
        laasForPayloadOgType(task.payload, task.type)

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

    /**
     * Tar en transaksjonsscopet Postgres advisory-lås (`pg_advisory_xact_lock`) nøkkelet på (type, payload), slik
     * at samtidige kall for nøyaktig samme skyggetask - fra denne eller andre podder - blir serialisert i stedet
     * for å forsøke duplikate inserts. Nøkkelen er en 32-bits hash av (type, payload); en eventuell hash-kollisjon
     * med en helt annen skyggetask fører i verste fall til at de kortvarig serialiseres unødvendig, ikke til noen
     * korrekthetsfeil. Låsen frigis automatisk når [Propagation.REQUIRES_NEW]-transaksjonen commit'er/ruller tilbake.
     */
    private fun laasForPayloadOgType(
        payload: String,
        type: String,
    ) {
        val låsnøkkel = "$type|$payload".hashCode()
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock($låsnøkkel)")
    }
}
