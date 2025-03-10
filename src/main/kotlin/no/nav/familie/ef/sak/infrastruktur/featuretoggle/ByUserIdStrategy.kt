package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory

class ByUserIdStrategy : Strategy {
    override fun getName(): String = "userWithId"

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun isEnabled(
        map: MutableMap<String, String>,
        unleashContext: UnleashContext,
    ): Boolean {
        secureLogger.warn("ByUserIdStrategy called with unused context: $unleashContext")
        return isEnabled(map)
    }

    fun isEnabled(map: MutableMap<String, String>): Boolean {

        secureLogger.warn("ByUserIdStrategy isEnabled map: $map")

        if (SikkerhetContext.erSystembruker()) {
            return false
        }
        return map["userIds"]
            ?.split(',')
            ?.any { SikkerhetContext.hentSaksbehandler() == it }
            ?: false
    }
}
