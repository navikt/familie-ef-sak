package no.nav.familie.ef.sak.selvstendig

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("!integrasjonstest")
@Component
class SelvstendigScheduler(
    val næringsinntektKontrollService: NæringsinntektKontrollService,
) : ApplicationListener<ApplicationReadyEvent> {
    // @Scheduled(cron = "0 0 0 * * ?") - blir en scheduler
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        næringsinntektKontrollService.sjekkNæringsinntektMotForventetInntekt()
    }
}
