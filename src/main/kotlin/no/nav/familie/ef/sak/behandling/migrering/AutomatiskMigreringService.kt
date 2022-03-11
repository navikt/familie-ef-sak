package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskMigreringService(private val migreringsstatusRepository: MigreringsstatusRepository,
                                 private val migreringService: MigreringService,
                                 private val infotrygdReplikaClient: InfotrygdReplikaClient,
                                 private val taskRepository: TaskRepository) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun migrerAutomatisk(antall: Int) {
        val personerForMigrering = infotrygdReplikaClient.hentPersonerForMigrering(500)
        val alleredeMigrert = migreringsstatusRepository.findAllByIdentIn(personerForMigrering).map { it.ident }

        val filtrerteIdenter = personerForMigrering.filterNot { alleredeMigrert.contains(it) }
                .take(antall) // henter fler fra infotrygd enn vi skal migrere, men plukker ut første X antall

        logger.info("Oppretter ${filtrerteIdenter.size} tasks for å migrere automatisk")
        migreringsstatusRepository.insertAll(filtrerteIdenter.map { Migreringsstatus(it, MigreringResultat.IKKE_KONTROLLERT) })
        taskRepository.saveAll(filtrerteIdenter.map {
            AutomatiskMigreringTask.opprettTask(it).apply { this.metadata[MDCConstants.MDC_CALL_ID] = IdUtils.generateId() }
        })
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun migrerPersonAutomatisk(personIdent: String) {
        val migreringStatus = migreringsstatusRepository.findByIdOrThrow(personIdent)
        if (migreringStatus.status == MigreringResultat.OK) {
            secureLogger.info("Allerede migrert")
            return
        }
        try {
            secureLogger.info("Automatisk migrering av ident=$personIdent")
            migreringService.migrerOvergangsstønadAutomatisk(personIdent)
            migreringsstatusRepository.update(migreringStatus.copy(status = MigreringResultat.OK))
            secureLogger.info("Automatisk migrering av ident=$personIdent utført=OK")
        } catch (e: MigreringException) {
            secureLogger.warn("Kan ikke migrere ident=$personIdent årsak=${e.type} msg=${e.årsak}")
            migreringsstatusRepository.update(migreringStatus.copy(status = MigreringResultat.FEILET, årsak = e.type))
        }
    }
}
