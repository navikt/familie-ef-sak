package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.unleash.UnleashContextFields
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class UnleashNextService(
    private val unleashService: UnleashService,
) {
    fun isEnabled(toggle: Toggle): Boolean {
        val unleashContextFieldsMap =
            if (SikkerhetContext.erSystembruker()) {
                emptyMap()
            } else {
                mapOf(
                    UnleashContextFields.NAV_IDENT to SikkerhetContext.hentSaksbehandler(),
                )
            }

        return unleashService.isEnabled(
            toggleId = toggle.toggleId,
            properties = unleashContextFieldsMap,
        )
    }
}
