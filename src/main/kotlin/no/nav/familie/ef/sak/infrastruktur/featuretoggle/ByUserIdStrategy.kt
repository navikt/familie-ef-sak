package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.strategy.Strategy
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext

class ByUserIdStrategy : Strategy {
    override fun getName(): String = "userWithId"

    override fun isEnabled(map: MutableMap<String, String>): Boolean {
        if (SikkerhetContext.erSystembruker()) {
            return false
        }
        return map["userIds"]
            ?.split(',')
            ?.any { SikkerhetContext.hentSaksbehandler() == it }
            ?: false
    }
}
