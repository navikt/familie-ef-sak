package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByEnvironmentStrategy : Strategy {
    companion object {
        private const val MILJØ_KEY = "miljø"
    }

    override fun getName(): String = "byEnvironment"

    fun isEnabled(map: Map<String, String>): Boolean = isEnabled(map, UnleashContext.builder().build())

    override fun isEnabled(
        map: Map<String, String>,
        unleashContext: UnleashContext,
    ): Boolean =
        unleashContext.environment
            .map { env -> map[MILJØ_KEY]?.split(',')?.contains(env) ?: false }
            .orElse(false)
}
