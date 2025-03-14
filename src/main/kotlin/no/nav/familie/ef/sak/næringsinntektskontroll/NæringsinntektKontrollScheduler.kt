package no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggle
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Profile("!integrasjonstest")
@Service
class NæringsinntektKontrollScheduler(
    val næringsinntektKontrollService: NæringsinntektKontrollService,
    val featureToggleService: FeatureToggleService,
) {
    @Scheduled(initialDelay = 60 * 1000L, fixedDelay = 365 * 24 * 60 * 60 * 1000L) // Kjører ved oppstart av app
    fun sjekkNæringsinntekt() {
        if (featureToggleService.isEnabled(FeatureToggle.KontrollerNæringsinntekt)) {
            næringsinntektKontrollService.opprettTasksForSelvstendigeTilInntektskontroll()
        }
    }
}
