package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.unleash.UnleashContextFields
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class UnleashNextService(
    private val unleashService: UnleashService,
) {
    fun isEnabled(toggle: Toggle): Boolean {
        val saksbehandlerEllerSystembruker = SikkerhetContext.hentSaksbehandlerEllerSystembruker()

        val unleashContextFieldsMap = if (saksbehandlerEllerSystembruker != SYSTEM_FORKORTELSE) {
            mapOf(UnleashContextFields.NAV_IDENT to saksbehandlerEllerSystembruker)
        } else {
            emptyMap()
        }

        return unleashService.isEnabled(
            toggleId = toggle.toggleId,
            properties = unleashContextFieldsMap,
        )
    }
}
