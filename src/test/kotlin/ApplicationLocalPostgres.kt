package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.infrastruktur.util.MockOAuth2ServerInitializer
import org.springframework.boot.builder.SpringApplicationBuilder
import java.util.Properties

class ApplicationLocalPostgres : ApplicationLocalSetup()

fun main(args: Array<String>) {
    val properties = Properties()
    properties["DATASOURCE_URL"] = "jdbc:postgresql://localhost:5432/familie-ef-sak"
    properties["DATASOURCE_USERNAME"] = "postgres"
    properties["DATASOURCE_PASSWORD"] = "test"
    properties["DATASOURCE_DRIVER"] = "org.postgresql.Driver"

    SpringApplicationBuilder(ApplicationLocalPostgres::class.java)
        .initializers(MockOAuth2ServerInitializer())
        .profiles(
            "local",
            "mock-arbeidssøker",
            "mock-integrasjoner",
            "mock-pdl",
            "mock-infotrygd-replika",
            "mock-kodeverk",
            "mock-iverksett",
            "mock-inntekt",
            "mock-ereg",
            "mock-aareg",
            "mock-brev",
            "mock-dokument",
            "mock-tilbakekreving",
            "mock-klage",
            "mock-sigrun",
            "mock-historiskpensjon",
            "mock-featuretoggle",
            "mock-egen-ansatt",
            "mock-kontantstøtte",
            "mock-fullmakt",
            "mock-medl",
        ).properties(properties)
        .run(*args)
}
