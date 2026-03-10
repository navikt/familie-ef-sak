package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.sikkerhet.EksternBrukerUtils
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object SikkerhetContext {
    private const val SYSTEM_NAVN = "System"
    const val SYSTEM_FORKORTELSE = "VL"
    private const val NAV_GROUPS_HEADER = "Nav-Groups"
    private const val NAV_IDENT_HEADER = "Nav-Ident"
    private const val NAV_USER_NAME_HEADER = "Nav-User-Name"

    val NAVIDENT_REGEX = """^[a-zA-Z]\d{6}$""".toRegex()

    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val objectMapper = jacksonObjectMapper()

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

    fun hentSaksbehandlerEllerSystembruker(): String {
        val navIdentFraHeader = hentNavIdentFraHeader()
        if (navIdentFraHeader != null) {
            secureLogger.info("[DEBUG] hentSaksbehandlerEllerSystembruker fra header: $navIdentFraHeader")
            return navIdentFraHeader
        }

        return try {
            val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
            val navIdent = claims?.get("NAVident")?.toString()
            secureLogger.info("[DEBUG] hentSaksbehandlerEllerSystembruker fra token: $navIdent")
            navIdent ?: SYSTEM_FORKORTELSE
        } catch (e: Exception) {
            secureLogger.info("[DEBUG] hentSaksbehandlerEllerSystembruker exception: ${e.message}")
            SYSTEM_FORKORTELSE
        }
    }

    private fun hentNavIdentFraHeader(): String? =
        try {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            val header = requestAttributes?.request?.getHeader(NAV_IDENT_HEADER)
            secureLogger.info("[DEBUG] hentNavIdentFraHeader: $header")
            header
        } catch (e: Exception) {
            secureLogger.info("[DEBUG] hentNavIdentFraHeader exception: ${e.message}")
            null
        }

    fun hentSaksbehandlerNavn(strict: Boolean = false): String {
        val navnFraHeader = hentNavnFraHeader()
        if (navnFraHeader != null) {
            return navnFraHeader
        }

        return try {
            val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
            claims?.get("name")?.toString()
                ?: if (strict) error("Finner ikke navn i azuread token") else SYSTEM_NAVN
        } catch (e: Exception) {
            if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN
        }
    }

    private fun hentNavnFraHeader(): String? =
        try {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            requestAttributes?.request?.getHeader(NAV_USER_NAME_HEADER)
        } catch (e: Exception) {
            null
        }

    fun hentGrupperFraToken(): Set<String> {
        val grupperFraHeader = hentGrupperFraHeader()
        if (grupperFraHeader.isNotEmpty()) {
            return grupperFraHeader
        }

        return try {
            val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")

            @Suppress("UNCHECKED_CAST")
            val groups = claims?.get("groups") as List<String>?
            groups?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun hentGrupperFraHeader(): Set<String> =
        Result
            .runCatching {
                val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
                val header = requestAttributes?.request?.getHeader(NAV_GROUPS_HEADER)
                secureLogger.info("[DEBUG] hentGrupperFraHeader raw: $header")
                if (header != null) {
                    val groups = objectMapper.readValue<List<String>>(header).toSet()
                    secureLogger.info("[DEBUG] hentGrupperFraHeader parsed: ${groups.size} grupper")
                    groups
                } else {
                    emptySet()
                }
            }.getOrElse {
                secureLogger.info("[DEBUG] hentGrupperFraHeader exception: ${it.message}")
                emptySet()
            }

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
