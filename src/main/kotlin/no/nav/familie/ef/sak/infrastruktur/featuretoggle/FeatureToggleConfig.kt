package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.strategy.Strategy
import no.nav.familie.unleash.DefaultUnleashService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FeatureToggleConfig(
    @Value("\${UNLEASH_SERVER_API_URL}") private val apiUrl: String,
    @Value("\${UNLEASH_SERVER_API_TOKEN}") private val apiToken: String,
    @Value("\${NAIS_APP_NAME}") private val appName: String,
) {

    @Bean
    fun strategies(): List<Strategy> {
        return listOf(ByUserIdStrategy(), ByEnvironmentStrategy())
    }

    @Profile("!mock-featuretoggle")
    @Bean
    fun defaultUnleashService(strategies: List<Strategy>): DefaultUnleashService {
        return DefaultUnleashService(apiUrl, apiToken, appName, strategies)
    }
}
