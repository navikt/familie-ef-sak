package no.nav.familie.ef.sak.selvstendig

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Profile("!integrasjonstest")
@Service
class SelvstendigScheduler(
    val selvstendigService: SelvstendigService,
) {
    @Scheduled(cron = "0 0/1 * * * *")
    fun inntektskontrollForSelvstendige() {
        selvstendigService.hentOppgaver()
    }
}