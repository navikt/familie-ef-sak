package no.nav.familie.ef.sak.no.nav.familie.ef.sak

import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

@TestConfiguration
class TestRestTemplateConfig {
    @Bean
    fun testRestTemplate(
        builder: RestTemplateBuilder,
    ): TestRestTemplate {
        val restTemplateBuilder =
            builder
                .additionalMessageConverters(
                    MappingJackson2HttpMessageConverter(ObjectMapperProvider.objectMapper),
                )
        return TestRestTemplate(restTemplateBuilder)
    }
}
