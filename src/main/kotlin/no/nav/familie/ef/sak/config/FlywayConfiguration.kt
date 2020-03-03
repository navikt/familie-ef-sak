package no.nav.familie.ef.sak.config

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean

@ConfigurationProperties("spring.cloud.vault.database")
@ConstructorBinding
data class FlywayConfiguration(private val role: String) {

    @Bean
    fun flywayConfig(): FlywayConfigurationCustomizer {
        return FlywayConfigurationCustomizer { c: FluentConfiguration ->
            c.initSql(String.format("SET ROLE \"%s\"", role))
        }
    }
}
