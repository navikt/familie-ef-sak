package no.nav.familie.ef.sak.infrastruktur.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig(
    @Value("\${AUTHORIZATION_URL}")
    val authorizationUrl: String,
    @Value("\${AZUREAD_TOKEN_ENDPOINT_URL}")
    val tokenUrl: String,
    @Value("\${API_SCOPE}")
    val apiScope: String,
) {
    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .components(Components().addSecuritySchemes("oauth2", securitySchemes()))
            .addSecurityItem(SecurityRequirement().addList("oauth2", listOf("read", "write")))
    }

    @Bean
    fun eksternOpenApi(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Ekstern").packagesToScan("no.nav.familie.ef.sak.ekstern").build()
    }

    @Bean
    fun internOpenApi(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Alle").packagesToScan("no.nav.familie.ef.sak").build()
    }

    @Bean
    fun forvalterOpenApi(): GroupedOpenApi {
        return GroupedOpenApi.builder().group("Forvalter").packagesToScan("no.nav.familie.ef.sak.forvaltning").build()
    }

    private fun securitySchemes(): SecurityScheme {
        return SecurityScheme()
            .name("oauth2")
            .type(SecurityScheme.Type.OAUTH2)
            .scheme("oauth2")
            .`in`(SecurityScheme.In.HEADER)
            .flows(
                OAuthFlows()
                    .authorizationCode(
                        OAuthFlow().authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(Scopes().addString(apiScope, "read,write")),
                    ),
            )
    }
}
