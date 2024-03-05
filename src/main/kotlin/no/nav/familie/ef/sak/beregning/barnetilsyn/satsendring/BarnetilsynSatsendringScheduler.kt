package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Profile("!integrasjonstest")
@Service
class BarnetilsynSatsendringScheduler(val barnetilsynSatsendringService: BarnetilsynSatsendringService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    //@Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L) // Kjører ved oppstart av app
    fun opprettTask() {
        logger.info("Starter scheduler for satsendring av barnetilsyn")
        barnetilsynSatsendringService.opprettTask()
    }
}
