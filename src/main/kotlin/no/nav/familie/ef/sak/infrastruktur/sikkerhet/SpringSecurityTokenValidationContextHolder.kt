package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class SpringSecurityTokenValidationContextHolder(
    @Value("\${AZURE_OPENID_CONFIG_ISSUER}") private val azureIssuer: String,
) : TokenValidationContextHolder {
    override fun getTokenValidationContext(): TokenValidationContext {
        val authentication =
            SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
                ?: error("No JWT authentication in SecurityContext")

        val jwt = authentication.token
        val issuerKey = if (jwt.issuer.toString() == azureIssuer) "azuread" else "tokenx"
        return TokenValidationContext(mapOf(issuerKey to JwtToken(jwt.tokenValue)))
    }

    override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext?) {
        // no-op
    }
}
