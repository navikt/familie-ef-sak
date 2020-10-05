package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocalPostgres

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5432/familie-ef-sak"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local",
                      "mock-integrasjoner",
                      "mock-pdl",
                      "mock-kodeverk")
            .properties(properties)
            .run(*args)
}