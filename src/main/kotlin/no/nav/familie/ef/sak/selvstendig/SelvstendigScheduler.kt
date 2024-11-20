package no.nav.familie.ef.sak.selvstendig

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Profile("!integrasjonstest")
@Service
class SelvstendigScheduler(
    val næringsinntektKontrollService: NæringsinntektKontrollService,
){

    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L) // Kjører ved oppstart av app
    fun sjekkNæringsinntekt() {
        næringsinntektKontrollService.sjekkNæringsinntektMotForventetInntekt()
    }
}
