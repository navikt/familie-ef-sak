package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class DbMigreringService(private val dbMigreringRepository: TilkjentYtelseRepository) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 120000, fixedDelay = ÅR)
    @Transactional
    fun dbMigrering() {
        val andeler = dbMigreringRepository.findAll()
                .map { tilkjentYtelse ->
                    tilkjentYtelse.copy(andelerTilkjentYtelse =
                                        tilkjentYtelse.andelerTilkjentYtelse.map {
                                            it.copy(id = UUID.randomUUID())
                                        })
                }
        dbMigreringRepository.updateAll(andeler)
        logger.info("Oppdatering av andeler utført")
    }

    companion object {

        const val ÅR = 1000 * 60 * 60 * 24 * 365L
    }
}
