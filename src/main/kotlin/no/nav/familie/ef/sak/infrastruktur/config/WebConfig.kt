package no.nav.familie.ef.sak.infrastruktur.config

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val tilgangInterceptor: TilgangInterceptor,
) : WebMvcConfigurer {
    private val excludePatterns =
        listOf(
            "/api/task/**",
            "/api/v2/task/**",
            "/internal/**",
            "/swagger-resources/**",
            "/swagger-resources",
            "/swagger-ui/**",
            "/swagger-ui",
            "/v3/api-docs/**",
            "/v3/api-docs",
            "/api/ekstern/perioder/full-overgangsstonad", // håndteres i controllern
            "/api/ekstern/minside/stonadsperioder", // urelevant å sjekke roller for tokenx/kall fra min-side
        )

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(tilgangInterceptor).excludePathPatterns(excludePatterns)
        super.addInterceptors(registry)
    }
}
