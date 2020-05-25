package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.common.KPostgreSQLContainer
import no.nav.familie.ef.sak.config.ApplicationConfig
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Import
import java.util.*

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@ConfigurationPropertiesScan
@Import(TokenGeneratorConfiguration::class)
class ApplicationLocalPostgres

fun main(args: Array<String>) {

    val psql = KPostgreSQLContainer("postgres")
            .withDatabaseName("familie-oppdrag")
            .withUsername("postgres")
            .withPassword("test")

    psql.start()

    val properties = Properties()
    properties.put("spring.datasource.url",psql.jdbcUrl)
    properties.put("spring.datasource.username", psql.username)
    properties.put("spring.datasource.password", psql.password)

    SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local-postgres",
                      "mock-integrasjoner",
                      "mock-oauth",
                      "mock-auth")
            .properties(properties)
            .run(*args)
}
