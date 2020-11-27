package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.config.ApplicationConfig
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.database.DbContainerInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(initializers = [DbContainerInitializer::class])
@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocal

fun main(args: Array<String>) {

    SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local",
                      "mock-integrasjoner",
                      "mock-pdl",
                      "mock-oppdrag",
                      "mock-kodeverk")
            .run(*args)
}
