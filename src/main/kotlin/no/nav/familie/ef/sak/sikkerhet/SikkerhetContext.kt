package no.nav.familie.ef.sak.sikkerhet

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {

    fun hentSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = { it.getClaims("azuread")?.get("preferred_username")?.toString() ?: "VL" },
                        onFailure = { "VL" }
                )
    }
}