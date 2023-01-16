package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.database.DbContainerInitializer
import no.nav.familie.ef.sak.infrastruktur.config.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocal

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationConfig::class.java)
        .initializers(DbContainerInitializer())
        .profiles(
            "local",
            "mock-arbeidssøker",
            "mock-integrasjoner",
            "mock-pdl",
            "mock-infotrygd-replika",
            "mock-kodeverk",
            "mock-blankett",
            "mock-iverksett",
            "mock-inntekt",
            "mock-ereg",
            "mock-aareg",
            "mock-brev",
            "mock-dokument",
            "mock-tilbakekreving",
            "mock-klage",
            "mock-sigrun",
            "mock-historiskpensjon"
        )
        .run(*args)
}
