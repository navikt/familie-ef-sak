package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.YearMonth

@Profile("!integrasjonstest")
@Service
class NæringsinntektKontrollScheduler(
    val næringsinntektKontrollService: NæringsinntektKontrollService,
    val featureToggleService: FeatureToggleService,
) {
    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L) // Kjører ved oppstart av app
    fun sjekkNæringsinntekt() {
        if (featureToggleService.isEnabled(Toggle.KONTROLLER_NÆRINGSINNTEKT)) {
            næringsinntektKontrollService.kontrollerInntektForSelvstendigNæringsdrivende(YearMonth.now().year - 1)
        }
    }
}
