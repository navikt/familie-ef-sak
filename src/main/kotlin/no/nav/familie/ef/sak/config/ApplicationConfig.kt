package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestOperations

@SpringBootConfiguration
@ConfigurationPropertiesScan
@Import(BearerTokenClientInterceptor::class,
        MdcValuesPropagatingClientInterceptor::class,
        ConsumerIdClientInterceptor::class)
class ApplicationConfig {

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule()

}
