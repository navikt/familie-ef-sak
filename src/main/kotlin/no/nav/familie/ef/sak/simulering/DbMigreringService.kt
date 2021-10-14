package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.infrastruktur.config.DatabaseConfiguration
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Deprecated("Kan fjernes når den er kjørt i produksjon")
@Service
class DbMigreringService(
        private val simuleringsresultatRepository: SimuleringsresultatRepository,
        private val jdbcTemplate: JdbcTemplate
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

        val converter = DatabaseConfiguration.BeriketSimuleringsresultatTilPGobjectConverter()

        iterable.forEach {
            val simuleringsoppsummering =
                    tilSimuleringsresultatDto(it.data, it.sporbar.endret.endretTid.toLocalDate())
                            .tilSimuleringsperiode()

            val beriketData =  BeriketSimuleringsresultat(it.data, simuleringsoppsummering)

            jdbcTemplate.update("update simuleringsresultat set beriket_data = ? where behandling_id = ?",
                                converter.convert(beriketData), it.behandlingId)
        }

        logger.info("Migrert ${iterable.count()} beriket_data for simuleringsresultat.")
    }
}
