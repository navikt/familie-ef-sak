package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object SikkerhetContext {
    private const val SYSTEM_NAVN = "System"
    const val SYSTEM_FORKORTELSE = "VL"

    val NAVIDENT_REGEX = """^[a-zA-Z]\d{6}$""".toRegex()

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private fun getJwtToken(): JwtAuthenticationToken? = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken

    fun erMaskinTilMaskinToken(): Boolean {
        val jwt = getJwtToken()?.token ?: return false
        val oid = jwt.getClaimAsString("oid")
        val sub = jwt.subject
        val roles = jwt.getClaimAsStringList("roles") ?: emptyList()
        return oid != null && oid == sub && roles.contains("access_as_application")
    }

    fun kallKommerFraFamilieEfMottak(): Boolean = kallKommerFra("teamfamilie:familie-ef-mottak")

    fun kallKommerFraKlage(): Boolean = kallKommerFra("teamfamilie:familie-klage")

    fun kallKommerFraFamilieEfSøknadApi(): Boolean {
        val jwt = getJwtToken()?.token ?: return false
        val applikasjonsnavn = jwt.getClaimAsString("client_id") ?: ""
        return applikasjonsnavn.endsWith("teamfamilie:familie-ef-soknad-api")
    }

    private fun kallKommerFra(forventetApplikasjonsSuffix: String): Boolean {
        val jwt = getJwtToken()?.token ?: return false
        val applikasjonsnavn = jwt.getClaimAsString("azp_name") ?: ""
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

    fun hentSaksbehandlerEllerSystembruker() = getJwtToken()?.token?.getClaimAsString("NAVident") ?: SYSTEM_FORKORTELSE

    fun hentSaksbehandlerNavn(strict: Boolean = false): String {
        val name = getJwtToken()?.token?.getClaimAsString("name")
        return name ?: if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN
    }

    fun hentGrupperFraToken(): Set<String> {
        val jwt = getJwtToken()?.token ?: return emptySet()
        return jwt.getClaimAsStringList("groups")?.toSet() ?: emptySet()
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
