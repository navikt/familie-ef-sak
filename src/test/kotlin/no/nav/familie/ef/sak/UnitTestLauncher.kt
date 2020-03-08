package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.config.ApplicationConfig
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@Import(TokenGeneratorConfiguration::class)
class UnitTestLauncher {

    fun main(args: Array<String>) {
        val app = SpringApplicationBuilder(ApplicationConfig::class.java)
                .profiles("local")
                .build()
        app.run(*args)
    }
}