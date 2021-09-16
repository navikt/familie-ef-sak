package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.infrastruktur.config.ApplicationConfig
import no.nav.familie.ef.sak.database.DbContainerInitializer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocal

fun main(args: Array<String>) {

    SpringApplicationBuilder(ApplicationConfig::class.java)
            .initializers(DbContainerInitializer())
            .profiles("local",
                      "mock-integrasjoner",
                      "mock-pdl",
                      "mock-infotrygd-replika",
                      "mock-kodeverk",
                      "mock-blankett",
                      "mock-iverksett",
                      "mock-brev")
            .run(*args)
}
