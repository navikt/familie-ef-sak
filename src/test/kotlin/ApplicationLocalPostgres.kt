package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.config.ApplicationConfig
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@ConfigurationPropertiesScan
@Import(TokenGeneratorConfiguration::class)
class ApplicationLocalPostgres

fun main(args: Array<String>) {

    val sqlContainer = KPostgreSQLContainer("postgres")
            .withDatabaseName("familie-oppdrag")
            .withUsername("postgres")
            .withPassword("test")

    sqlContainer.start()

    val properties = Properties()
    properties["spring.datasource.url"] = sqlContainer.jdbcUrl
    properties["spring.datasource.username"] = sqlContainer.username
    properties["spring.datasource.password"] = sqlContainer.password
    properties["spring.datasource.driver-class-name"] = "org.postgresql.Driver"

    val app = SpringApplicationBuilder(ApplicationConfig::class.java)
            .profiles("local-postgres",
                      "mock-integrasjoner",
                      "mock-pdl",
                      "mock-oauth",
                      "mock-auth")
            .build()

    app.setDefaultProperties(properties)
    app.run(*args)
}

// Hack needed because testcontainers use of generics confuses Kotlin
class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)