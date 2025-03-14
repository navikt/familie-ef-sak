package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import io.getunleash.strategy.Strategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomUnleashStrategies {
    @Bean
    fun strategies(): List<Strategy> = listOf(ByUserIdStrategy(), ByEnvironmentStrategy(), ByTargetingStrategy())
}
