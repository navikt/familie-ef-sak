package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByEnvironmentStrategy : Strategy {

    companion object {

        private const val miljøKey = "miljø"
    }

    override fun getName(): String {
        return "byEnvironment"
    }

    override fun isEnabled(map: Map<String, String>): Boolean {
        return isEnabled(map, UnleashContext.builder().build())
    }

    override fun isEnabled(map: Map<String, String>, unleashContext: UnleashContext): Boolean {
        return unleashContext.environment
            .map { env -> map[miljøKey]?.split(',')?.contains(env) ?: false }
            .orElse(false)
    }
}
