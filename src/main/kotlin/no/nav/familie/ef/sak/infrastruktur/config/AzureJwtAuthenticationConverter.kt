package no.nav.familie.ef.sak.infrastruktur.config

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.Rolle
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class AzureJwtAuthenticationConverter(
    private val rolleConfig: RolleConfig,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val groups = jwt.getClaimAsStringList("groups") ?: emptyList()
        val roles = jwt.getClaimAsStringList("roles") ?: emptyList()

        val roller =
            buildSet {
                if (roles.contains(ACCESS_AS_APPLICATION_ROLE)) add(Rolle.APPLICATION)
                if (groups.contains(rolleConfig.beslutterRolle)) add(Rolle.BESLUTTER)
                if (groups.contains(rolleConfig.saksbehandlerRolle)) add(Rolle.SAKSBEHANDLER)
                if (groups.contains(rolleConfig.veilederRolle)) add(Rolle.VEILEDER)
                if (groups.contains(rolleConfig.forvalter)) add(Rolle.FORVALTER)
            }

        val authorities = roller.map { SimpleGrantedAuthority(it.authority()) }
        return JwtAuthenticationToken(jwt, authorities)
    }

    companion object {
        private const val ACCESS_AS_APPLICATION_ROLE = "access_as_application"
    }
}
