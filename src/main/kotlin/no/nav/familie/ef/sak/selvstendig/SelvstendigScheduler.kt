package no.nav.familie.ef.sak.selvstendig

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Profile("!integrasjonstest")
@Service
class SelvstendigScheduler(
    val næringsinntektKontrollService: NæringsinntektKontrollService,
) {
    // @Scheduled(cron = "0 0 0 * * ?")
    @EventListener(ApplicationReadyEvent::class)
    fun inntektskontrollForSelvstendige() {
        næringsinntektKontrollService.sjekkNæringsinntektMotForventetInntekt()
    }
}

fun main() {
    val test = 0 / 1
    println(test)
}
