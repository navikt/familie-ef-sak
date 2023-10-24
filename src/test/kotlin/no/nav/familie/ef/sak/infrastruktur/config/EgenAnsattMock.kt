package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.egenansatt.EgenAnsattClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-egen-ansatt")
class EgenAnsattMock {

    @Bean
    @Primary
    fun egenAnsattClient(): EgenAnsattClient {
        val client = mockk<EgenAnsattClient>()
        every { client.egenAnsatt(any<String>()) } answers { firstArg<String>() == "ikkeTilgang" }
        return client
    }
}
