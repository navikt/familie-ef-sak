package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory

object SikkerhetContext {
    private const val SYSTEM_NAVN = "System"
    const val SYSTEM_FORKORTELSE = "VL"

    val NAVIDENT_REGEX = """^[a-zA-Z]\d{6}$""".toRegex()

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }

    fun kallKommerFraFamilieEfMottak(): Boolean = kallKommerFra("teamfamilie:familie-ef-mottak")

    fun kallKommerFraKlage(): Boolean = kallKommerFra("teamfamilie:familie-klage")

    fun kallKommerFraFamilieEfSøknadApi(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims(EksternBrukerUtils.ISSUER_TOKENX)
        val applikasjonsnavn = claims.get("client_id")?.toString() ?: "" // e.g. dev-gcp:some-team:application-name
        return applikasjonsnavn.endsWith("teamfamilie:familie-ef-soknad-api")
    }

    private fun kallKommerFra(forventetApplikasjonsSuffix: String): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        val applikasjonsnavn = claims.get("azp_name")?.toString() ?: "" // e.g. dev-gcp:some-team:application-name
        secureLogger.info("Applikasjonsnavn: $applikasjonsnavn")
        return applikasjonsnavn.endsWith(forventetApplikasjonsSuffix)
    }

    fun erSystembruker(): Boolean = hentSaksbehandlerEllerSystembruker() == SYSTEM_FORKORTELSE

    fun hentSaksbehandler(): String {
        val result = hentSaksbehandlerEllerSystembruker()

        if (result == SYSTEM_FORKORTELSE) {
            error("Finner ikke NAVident i token")
        }
        return result
    }

    fun hentSaksbehandlerEllerSystembruker() =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.getClaims("azuread")?.get("NAVident")?.toString() ?: SYSTEM_FORKORTELSE
                },
                onFailure = { SYSTEM_FORKORTELSE },
            )

    fun hentSaksbehandlerNavn(strict: Boolean = false): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.getClaims("azuread")?.get("name")?.toString()
                        ?: if (strict) error("Finner ikke navn i azuread token") else SYSTEM_NAVN
                },
                onFailure = { if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN },
            )

    fun hentGrupperFraToken(): Set<String> =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    val groups = it.getClaims("azuread")?.get("groups") as List<String>?
                    groups?.toSet() ?: emptySet()
                },
                onFailure = { emptySet() },
            )

    fun harTilgangTilGittRolle(
        rolleConfig: RolleConfig,
        minimumsrolle: BehandlerRolle,
    ): Boolean {
        val rollerFraToken = hentGrupperFraToken()
        val rollerForBruker =
            when {
                hentSaksbehandlerEllerSystembruker() == SYSTEM_FORKORTELSE -> {
                    listOf(
                        BehandlerRolle.SYSTEM,
                        BehandlerRolle.BESLUTTER,
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )
                }

                rollerFraToken.contains(rolleConfig.beslutterRolle) -> {
                    listOf(
                        BehandlerRolle.BESLUTTER,
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )
                }

                rollerFraToken.contains(rolleConfig.saksbehandlerRolle) -> {
                    listOf(
                        BehandlerRolle.SAKSBEHANDLER,
                        BehandlerRolle.VEILEDER,
                    )
                }

                rollerFraToken.contains(rolleConfig.veilederRolle) -> {
                    listOf(BehandlerRolle.VEILEDER)
                }

                else -> {
                    listOf(BehandlerRolle.UKJENT)
                }
            }

        return rollerForBruker.contains(minimumsrolle)
    }

    fun harRolle(rolleId: String): Boolean = hentGrupperFraToken().contains(rolleId)
}
