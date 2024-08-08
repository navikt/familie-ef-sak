package no.nav.familie.ef.sak

import no.nav.familie.ef.sak.database.DbContainerInitializer
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.boot.builder.SpringApplicationBuilder

@EnableMockOAuth2Server
class ApplicationLocal : ApplicationLocalSetup()

fun main(args: Array<String>) {
    SpringApplicationBuilder(ApplicationLocal::class.java)
        .initializers(DbContainerInitializer())
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
        ).run(*args)
}
