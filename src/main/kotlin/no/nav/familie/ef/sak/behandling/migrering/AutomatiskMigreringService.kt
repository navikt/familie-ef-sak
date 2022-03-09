package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutomatiskMigreringService(private val migreringsstatusRepository: MigreringsstatusRepository,
                                 private val migreringService: MigreringService,
                                 private val infotrygdReplikaClient: InfotrygdReplikaClient) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    // Denne skal ikke gjøres i en transaksjon, då hver migrering håndteres hver for seg
    fun migrerAutomatisk(antall: Int) {
        val personerForMigrering = infotrygdReplikaClient.hentPersonerForMigrering(500)
        val alleredeMigrert = migreringsstatusRepository.findAllByIdentIn(personerForMigrering).map { it.ident }
        val filtrerteIdenter = personerForMigrering.filterNot { alleredeMigrert.contains(it) }
                .take(antall) // henter fler fra infotrygd enn vi skal migrere, men plukker ut første X antall
        logger.info("Automatisk migrering - antall=$filtrerteIdenter")
        filtrerteIdenter.forEach { migrerPerson(it) }
        logger.info("Automatisk migrering utført")
    }

    private fun migrerPerson(personIdent: String) {
        try {
            secureLogger.info("Automatisk migrering av ident=$personIdent")
            migreringService.migrerOvergangsstønadAutomatisk(personIdent)
            migreringsstatusRepository.insert(Migreringsstatus(personIdent, MigreringResultat.OK))
            secureLogger.info("Automatisk migrering av ident=$personIdent utført=OK")
        } catch (e: MigreringException) {
            secureLogger.warn("Kan ikke migrere ident=$personIdent årsak=${e.type} msg=${e.årsak}")
            migreringsstatusRepository.insert(Migreringsstatus(personIdent, MigreringResultat.FEILET, årsak = e.type))
        } catch (e: Exception) {
            secureLogger.warn("Feilet migrering av ident=$personIdent årsak=UKJENT", e)
            migreringsstatusRepository.insert(Migreringsstatus(personIdent, MigreringResultat.FEILET))
        }
    }
}
