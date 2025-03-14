package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class UnleashNextService(
    private val unleashService: UnleashService,
) {
    fun isEnabled(toggle: Toggle): Boolean {
        val unleashContextFields = toggle.mapUnleashContextFields()

        return unleashService.isEnabled(
            toggleId = toggle.toggleId,
            properties = unleashContextFields,
        )
    }
}
