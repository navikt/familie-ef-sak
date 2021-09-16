package no.nav.familie.ef.sak.sikkerhet

import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.service.steg.BehandlerRolle
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object SikkerhetContext {

    private const val SYSTEM_NAVN = "System"
    private const val SYSTEM_FORKORTELSE = "VL"

    val NAVIDENT_REGEX = """^[a-zA-Z]\d{6}$""".toRegex()

    /**
     * @param strict hvis true - skal kaste feil hvis token ikke inneholder NAVident
     */
    fun hentSaksbehandler(strict: Boolean = false): String {
        val result = Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(onSuccess = {
                    it.getClaims("azuread")?.get("NAVident")?.toString() ?: SYSTEM_FORKORTELSE
                },
                      onFailure = { SYSTEM_FORKORTELSE })
        if (strict && result == SYSTEM_FORKORTELSE) {
            error("Finner ikke NAVident i token")
        }
        return result
    }

    fun hentSaksbehandlerMail(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(onSuccess = {
                    it.getClaims("azuread")?.get("preferred_username")?.toString() ?: SYSTEM_FORKORTELSE
                },
                      onFailure = { SYSTEM_FORKORTELSE })
    }

    fun hentSaksbehandlerNavn(strict: Boolean = false): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(onSuccess = {
                    it.getClaims("azuread")?.get("name")?.toString()
                    ?: if (strict) error("Finner ikke navn i azuread token") else SYSTEM_NAVN
                },
                      onFailure = { if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN })
    }

    private fun hentGruppeFraToken(): List<String> {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                },
                      onFailure = { emptyList() })
    }

    fun harTilgangTilGittRolle(rolleConfig: RolleConfig, minimumsrolle: BehandlerRolle): Boolean {
        val rollerFraToken = hentGruppeFraToken()
        val rollerForBruker = when {
            hentSaksbehandler() == SYSTEM_FORKORTELSE -> listOf(BehandlerRolle.SYSTEM,
                                                                BehandlerRolle.BESLUTTER,
                                                                BehandlerRolle.SAKSBEHANDLER,
                                                                BehandlerRolle.VEILEDER)
            rollerFraToken.contains(rolleConfig.beslutterRolle) -> listOf(BehandlerRolle.BESLUTTER,
                                                                          BehandlerRolle.SAKSBEHANDLER,
                                                                          BehandlerRolle.VEILEDER)
            rollerFraToken.contains(rolleConfig.saksbehandlerRolle) -> listOf(BehandlerRolle.SAKSBEHANDLER,
                                                                              BehandlerRolle.VEILEDER)
            rollerFraToken.contains(rolleConfig.veilederRolle) -> listOf(BehandlerRolle.VEILEDER)
            else -> listOf(BehandlerRolle.UKJENT)
        }

        return rollerForBruker.contains(minimumsrolle)
    }
}