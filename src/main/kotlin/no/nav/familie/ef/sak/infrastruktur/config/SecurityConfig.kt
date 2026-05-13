package no.nav.familie.ef.sak.infrastruktur.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    @Value("\${AZURE_OPENID_CONFIG_ISSUER}") private val azureIssuer: String,
    @Value("\${TOKEN_X_ISSUER}") private val tokenxIssuer: String,
    private val azureJwtAuthenticationConverter: AzureJwtAuthenticationConverter,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        val authManagerResolver = buildAuthManagerResolver()
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/internal/**",
                        "/actuator/**",
                        "/api/ping",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.oauth2ResourceServer { it.authenticationManagerResolver(authManagerResolver) }
            .addFilterAfter(TokenValidationContextFilter(azureIssuer), BearerTokenAuthenticationFilter::class.java)
        return http.build()
    }

    private fun buildAuthManagerResolver(): JwtIssuerAuthenticationManagerResolver {
        val azureManager by lazy {
            val decoder: JwtDecoder = JwtDecoders.fromIssuerLocation(azureIssuer)
            val provider =
                JwtAuthenticationProvider(decoder).also {
                    it.setJwtAuthenticationConverter(azureJwtAuthenticationConverter)
                }
            ProviderManager(provider)
        }
        val tokenxManager by lazy {
            val decoder: JwtDecoder = JwtDecoders.fromIssuerLocation(tokenxIssuer)
            ProviderManager(JwtAuthenticationProvider(decoder))
        }
        val resolver =
            AuthenticationManagerResolver<String> { issuer ->
                when (issuer) {
                    azureIssuer -> azureManager
                    tokenxIssuer -> tokenxManager
                    else -> error("Untrusted issuer: $issuer")
                }
            }
        return JwtIssuerAuthenticationManagerResolver(resolver)
    }
}

class TokenValidationContextFilter(
    private val azureIssuer: String,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authentication =
            org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .authentication
        if (authentication is JwtAuthenticationToken) {
            val jwt = authentication.token
            val issuerKey = if (jwt.issuer.toString() == azureIssuer) "azuread" else "tokenx"
            val context = TokenValidationContext(mapOf(issuerKey to JwtToken(jwt.tokenValue)))
            request.setAttribute(SPRING_TOKEN_VALIDATION_CONTEXT_ATTRIBUTE, context)
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val SPRING_TOKEN_VALIDATION_CONTEXT_ATTRIBUTE =
            "no.nav.security.token.support.spring.SpringTokenValidationContextHolder"
    }
}
