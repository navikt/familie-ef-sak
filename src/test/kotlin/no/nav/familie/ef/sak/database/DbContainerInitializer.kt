package no.nav.familie.ef.sak.database

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.PostgreSQLContainer

class DbContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        postgres.start()
    }

    companion object {
        // Lazy because we only want it to be initialized when accessed
        private val postgres: KPostgreSQLContainer by lazy {
            KPostgreSQLContainer("postgres:17.6")
                .withDatabaseName("ef-sak")
                .withUsername("postgres")
                .withPassword("test")
        }
    }
}

// Hack needed because testcontainers use of generics confuses Kotlin
class KPostgreSQLContainer(
    imageName: String,
) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)
