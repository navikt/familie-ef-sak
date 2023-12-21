package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class BarnetilsynSatsendringScheduler(val barnetilsynSatsendringService: BarnetilsynSatsendringService) {

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L) // Kjører ved oppstart av app
    fun opprettTask() {
        Thread.sleep(Random.nextLong(5_000)) // YOLO unngå feil med att 2 noder
        barnetilsynSatsendringService.opprettTask()
    }
}
