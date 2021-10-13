package no.nav.familie.ef.sak.simulering

import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Deprecated("Kan fjernes når den er kjørt i produksjon")
@Service
class DbMigreringService(
        private val simuleringsresultatRepository: SimuleringsresultatRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 120000, fixedDelay = 3600000)
    @Transactional
    fun dbMigrering() {

        val iterable = simuleringsresultatRepository.findWhereBeriketDataIsNull()

        if (iterable.none()) {
            logger.info("Migrering av beriket simulering er fullført.")
            return
        }

        iterable.forEach {
            val simuleringsoppsummering =
                    tilSimuleringsresultatDto(it.data, it.sporbar.endret.endretTid.toLocalDate())
                            .tilSimuleringsperiode()

            simuleringsresultatRepository.update(
                    it.copy(beriketData = BeriketSimuleringsresultat(it.data, simuleringsoppsummering)))
        }

        logger.info("Migrert  ${iterable.count()} beriket_data for simuleringsresultat.")
    }

}
