package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class UnleashNextService(
    private val unleashService: UnleashService,
) {
    fun isEnabled(featureToggle: FeatureToggle): Boolean {
        val unleashContextFields = featureToggle.mapUnleashContextFields()

        return unleashService.isEnabled(
            toggleId = featureToggle.toggleId,
            properties = unleashContextFields,
        )
    }
}
