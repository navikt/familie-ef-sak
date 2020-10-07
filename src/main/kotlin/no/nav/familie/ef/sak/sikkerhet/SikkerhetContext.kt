package no.nav.familie.ef.sak.sikkerhet

import no.nav.familie.ef.sak.service.steg.BehandlerRolle
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {

    const val SYSTEM_NAVN = "System"
    const val SYSTEM_FORKORTELSE = "VL"

    fun hentSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("preferred_username")?.toString() ?: SYSTEM_FORKORTELSE },
                        onFailure = { SYSTEM_FORKORTELSE }
                )
    }

    fun hentSaksbehandlerNavn(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("name")?.toString() ?: SYSTEM_NAVN },
                        onFailure = { SYSTEM_NAVN }
                )
    }

    fun hentBehandlerRolleForSteg(lavesteSikkerhetsnivå: BehandlerRolle?): BehandlerRolle {
        if (hentSaksbehandler() == SYSTEM_FORKORTELSE) return BehandlerRolle.SYSTEM

        val høyesteSikkerhetsnivåForInnloggetBruker: BehandlerRolle = BehandlerRolle.BESLUTTER // TODO: Implementer dette når vi har roller i AD


        return when {
            lavesteSikkerhetsnivå == null -> BehandlerRolle.UKJENT
            høyesteSikkerhetsnivåForInnloggetBruker.nivå >= lavesteSikkerhetsnivå.nivå -> lavesteSikkerhetsnivå
            else -> BehandlerRolle.UKJENT
        }
    }
}