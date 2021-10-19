package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.finn.unleash.strategy.Strategy
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext

class ByUserIdStrategy : Strategy {

    override fun getName(): String {
        return "byUserId"
    }

    override fun isEnabled(map: MutableMap<String, String>): Boolean {
        return map["user"]
                       ?.split(',')
                       ?.any { SikkerhetContext.hentSaksbehandler(strict = true) == it }
               ?: false
    }
}