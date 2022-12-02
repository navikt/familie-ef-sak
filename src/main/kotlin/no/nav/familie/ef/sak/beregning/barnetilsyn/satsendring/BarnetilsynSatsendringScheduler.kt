package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.random.Random

@Profile("!integrasjonstest")
@Service
class BarnetilsynSatsendringScheduler(val barnetilsynSatsendringService: BarnetilsynSatsendringService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L) // TODO Slettes
    fun opprettTask() {
        logger.info("Oppretter satsendring-task")
        Thread.sleep(Random.nextLong(5_000)) // YOLO unngå feil med att 2 noder
        barnetilsynSatsendringService.opprettTask()
    }
}
