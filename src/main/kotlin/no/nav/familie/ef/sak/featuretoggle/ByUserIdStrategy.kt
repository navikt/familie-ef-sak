package no.nav.familie.ef.sak.featuretoggle

import no.finn.unleash.strategy.Strategy
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory

class ByUserIdStrategy : Strategy {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun getName(): String {
        return "byUserId";
    }

    override fun isEnabled(map: MutableMap<String, String>): Boolean {
        secureLogger.info("map values " +  map.values)
        secureLogger.info("map keys " +  map.keys)
        return map["user"]
                       ?.split(',')
                       ?.any { SikkerhetContext.hentSaksbehandler(strict = true) === it }
               ?: false
    }
}