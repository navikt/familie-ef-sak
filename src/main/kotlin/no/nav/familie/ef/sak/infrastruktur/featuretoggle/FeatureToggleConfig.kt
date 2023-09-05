package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.strategy.Strategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeatureToggleConfig {

    @Bean
    fun strategies(): List<Strategy> {
        return listOf(ByUserIdStrategy(), ByEnvironmentStrategy())
    }
}