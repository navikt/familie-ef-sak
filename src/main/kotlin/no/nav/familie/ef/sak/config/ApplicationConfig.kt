package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.familie.http.config.RestTemplateAzure
import no.nav.familie.http.interceptor.ApiKeyInjectingClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.http.interceptor.StsBearerTokenClientInterceptor
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.filter.LogFilter
import no.nav.familie.log.filter.RequestTimeFilter
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.spring.oauth2.DefaultOAuth2HttpClient
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.core.configuration.ProxyAwareResourceRetriever
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit


@SpringBootConfiguration
@ConfigurationPropertiesScan
@ComponentScan("no.nav.familie.prosessering", "no.nav.familie.ef.sak", "no.nav.familie.sikkerhet")
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation.swagger"])
@Import(RestTemplateAzure::class, StsBearerTokenClientInterceptor::class, StsRestClient::class)
@EnableOAuth2Client(cacheEnabled = true)
@EnableScheduling
class ApplicationConfig {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun kotlinModule(): KotlinModule = KotlinModule()

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        logger.info("Registering LogFilter filter")
        val filterRegistration = FilterRegistrationBean<LogFilter>()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    @Bean
    fun requestTimeFilter(): FilterRegistrationBean<RequestTimeFilter> {
        logger.info("Registering RequestTimeFilter filter")
        val filterRegistration = FilterRegistrationBean<RequestTimeFilter>()
        filterRegistration.filter = RequestTimeFilter()
        filterRegistration.order = 2
        return filterRegistration
    }

    /**
     * Overskrever felles sin som bruker proxy, som ikke skal brukes på gcp
     */
    @Bean
    @Primary
    fun restTemplateBuilder(): RestTemplateBuilder {
        val jackson2HttpMessageConverter = MappingJackson2HttpMessageConverter(objectMapper)
        return RestTemplateBuilder()
                .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(30, ChronoUnit.SECONDS))
                .additionalMessageConverters(listOf(jackson2HttpMessageConverter) + RestTemplate().messageConverters)
    }

    @Bean("utenAuth")
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder,
                     consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestOperations {
        return restTemplateBuilder.additionalInterceptors(consumerIdClientInterceptor,
                                                          MdcValuesPropagatingClientInterceptor()).build()
    }

    @Bean
    fun apiKeyInjectingClientInterceptor(@Value("\${PDL_APIKEY}") pdlApiKey: String,
                                         @Value("\${PDL_URL}") pdlBaseUrl: String): ApiKeyInjectingClientInterceptor {
        val map = mapOf(Pair(URI.create(pdlBaseUrl), Pair(API_KEY_HEADER, pdlApiKey)))
        return ApiKeyInjectingClientInterceptor(map)
    }

    @Bean("stsMedApiKey")
    fun restTemplateSts(stsBearerTokenClientInterceptor: StsBearerTokenClientInterceptor,
                        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                        apiKeyInjectingClientInterceptor: ApiKeyInjectingClientInterceptor): RestOperations {
        return RestTemplateBuilder()
                .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(15, ChronoUnit.SECONDS))
                .additionalInterceptors(consumerIdClientInterceptor,
                                        stsBearerTokenClientInterceptor,
                                        apiKeyInjectingClientInterceptor,
                                        MdcValuesPropagatingClientInterceptor()
                ).build()
    }

    // Brukes for sts issuer som brukes for sts validering. ApiKey blir lagt til når man henter metadata for STS_DISCOVERY_URL
    // trenger override pga token-support-test som allerede overridear denne i test scope
    @Bean
    @Primary
    @Profile("!integrasjonstest && !local")
    fun oidcResourceRetriever(@Value("\${STS_APIKEY}") stsApiKey: String): ProxyAwareResourceRetriever {
        val proxyAwareResourceRetriever = ProxyAwareResourceRetriever(null, false)
        proxyAwareResourceRetriever.headers = mapOf(API_KEY_HEADER to listOf(stsApiKey))
        return proxyAwareResourceRetriever
    }

    /**
     * Overskrever OAuth2HttpClient som settes opp i token-support som ikke kan få med objectMapper fra felles
     * pga .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
     * og [OAuth2AccessTokenResponse] som burde settes med setters, då feltnavn heter noe annet enn feltet i json
     */
    @Bean
    @Primary
    fun oAuth2HttpClient(): OAuth2HttpClient {
        return DefaultOAuth2HttpClient(RestTemplateBuilder()
                                               .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                                               .setReadTimeout(Duration.of(4, ChronoUnit.SECONDS)))
    }

    companion object {

        private const val API_KEY_HEADER = "x-nav-apiKey"
    }
}
