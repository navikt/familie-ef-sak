package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-infotrygd-replika")
class InfotrygdReplikaMock {

    @Bean
    @Primary
    fun infotrygdReplikaClient(): InfotrygdReplikaClient {
        val client = mockk<InfotrygdReplikaClient>()
        every { client.hentPerioderOvergangsstønad(any()) } returns InfotrygdPerioderOvergangsstønadResponse(emptyList())
        every { client.hentInslagHosInfotrygd(any()) } answers { InfotrygdFinnesResponse(emptyList(), emptyList()) }
        return client
    }

}