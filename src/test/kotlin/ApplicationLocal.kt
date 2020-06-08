package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.config.ApplicationConfig
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Import

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@ConfigurationPropertiesScan
@Import(TokenGeneratorConfiguration::class)
class ApplicationLocal

fun main(args: Array<String>) {

    SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local",
                      "mock-integrasjoner",
                      "mock-pdl",
                      "mock-oauth",
                      "mock-auth",
                      "mock-kodeverk")
            .run(*args)
}
