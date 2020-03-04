package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.familie.http.config.RestTemplateAzure
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@ConfigurationPropertiesScan
@Import(RestTemplateAzure::class)
@EnableJwtTokenValidation
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule()

}
