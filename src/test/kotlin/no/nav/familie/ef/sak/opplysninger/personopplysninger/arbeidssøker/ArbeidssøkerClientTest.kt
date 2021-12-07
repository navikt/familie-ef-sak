package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import io.mockk.every
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Profile("mock-arbeidssøker")
@Configuration
internal class ArbeidssøkerConfiguration {

    @Bean
    fun arbeidssøkerClient(): ArbeidssøkerClient {
        val client: ArbeidssøkerClient = mockk()

        every { client.hentPerioder(any(), any(), any()) } answers {
            ArbeidssøkerResponse(listOf(ArbeidssøkerPeriode(secondArg<LocalDate>().minusMonths(1), LocalDate.now().plusDays(1))))
        }

        return client
    }
}