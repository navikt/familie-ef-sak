package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("!integrasjonstest")
@Service
class BarnetilsynSatsendringScheduler(
    val barnetilsynSatsendringService: BarnetilsynSatsendringService,
) {
    private val logger = Logg.getLogger(this::class)

    // @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L) // Kjører ved oppstart av app
    fun opprettTask() {
        logger.info("Starter scheduler for satsendring av barnetilsyn")
        barnetilsynSatsendringService.opprettTask()
    }
}
