package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.infrastruktur.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Import

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@Import(ApplicationConfig::class)
class UnitTestLauncher {
    fun main(args: Array<String>) {
        val app =
            SpringApplicationBuilder(ApplicationConfig::class.java)
                .profiles(
                    "local",
                    "mock-oauth",
                ).build()
        app.run(*args)
    }
}
