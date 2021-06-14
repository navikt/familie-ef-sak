package no.nav.familie.ef.sak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.*

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocalPostgres

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5432/familie-ef-sak"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    SpringApplicationBuilder(ApplicationLocalPostgres::class.java)
            .profiles("local",
                      "mock-integrasjoner",
                      "mock-pdl",
                      "mock-infotrygd-replika",
                      "mock-kodeverk",
                      "mock-blankett",
                      "mock-iverksett",
                      "mock-brev")
            .properties(properties)
            .run(*args)
}