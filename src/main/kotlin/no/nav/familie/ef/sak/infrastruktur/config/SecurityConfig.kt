package no.nav.familie.ef.sak.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    @Value("\${AZURE_OPENID_CONFIG_ISSUER}") private val azureIssuer: String,
    @Value("\${TOKEN_X_ISSUER}") private val tokenxIssuer: String,
    private val azureJwtAuthenticationConverter: AzureJwtAuthenticationConverter,
) {
    @Bean
    @Order(1)
    fun tokenXSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/api/ekstern/soknad/**", "/api/ekstern/minside/**")
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .oauth2ResourceServer { it.jwt { jwt -> jwt.decoder(JwtDecoders.fromIssuerLocation(tokenxIssuer)) } }
            .build()

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun azureSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it
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
            }.oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.decoder(JwtDecoders.fromIssuerLocation(azureIssuer))
                    jwt.jwtAuthenticationConverter(azureJwtAuthenticationConverter)
                }
            }.httpBasic { it.disable() }
            .build()
}
