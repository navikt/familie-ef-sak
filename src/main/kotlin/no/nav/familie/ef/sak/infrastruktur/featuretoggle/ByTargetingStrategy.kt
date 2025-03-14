package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByTargetingStrategy : Strategy {
    override fun getName() = "flexibleRollout"

    override fun isEnabled(
        map: MutableMap<String, String>,
        unleashContext: UnleashContext,
    ): Boolean {
        val brukerId = unleashContext.userId.orElse(null)

        return if (brukerId != null) {
            map[brukerId]?.toBoolean() ?: false
        } else {
            false
        }
    }
}
