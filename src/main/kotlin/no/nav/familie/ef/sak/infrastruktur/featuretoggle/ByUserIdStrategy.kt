package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.strategy.Strategy
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory

class ByUserIdStrategy : Strategy {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getName(): String {
        return "byUserId"
    }

    override fun isEnabled(map: MutableMap<String, String>): Boolean {
        return map["user"]
            ?.split(',')
            ?.any {
                logger.info("ByUserId: $it")
                SikkerhetContext.hentSaksbehandler() == it
            }
            ?: false
    }
}
