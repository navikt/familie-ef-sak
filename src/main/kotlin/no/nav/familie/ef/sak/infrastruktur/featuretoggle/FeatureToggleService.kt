package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
    val unleashNextService: UnleashNextService,
) {
    fun isEnabled(featureToggle: FeatureToggle) = unleashNextService.isEnabled(featureToggle)
}
