package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestOperations
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootConfiguration
@ConfigurationPropertiesScan
@EntityScan("no.nav.familie")
@ComponentScan("no.nav.familie")
@EnableSwagger2
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger.web.ApiResourceController"])
//@EnableOAuth2Client(cacheEnabled = true)
class ApplicationConfig {

    @Bean
    fun restTemplate(vararg interceptors: ClientHttpRequestInterceptor): RestOperations =
            RestTemplateBuilder().interceptors(*interceptors).build()

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule()

}
