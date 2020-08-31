package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.familie.http.config.RestTemplateAzure
import no.nav.familie.http.config.RestTemplateSts
import no.nav.familie.http.sts.StsRestClient
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootConfiguration
@ConfigurationPropertiesScan
@ComponentScan("no.nav.familie.ef.sak", "no.nav.familie.sikkerhet")
@EnableSwagger2
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger"])
@Import(RestTemplateAzure::class, RestTemplateSts::class, StsRestClient::class)
@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule()

}
