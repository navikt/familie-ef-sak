package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import io.mockk.every
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Profile("mock-arbeidssøker")
@Configuration
internal class ArbeidssøkerConfiguration {
    @Bean
    fun arbeidssøkerClient(): ArbeidssøkerClient {
        val client: ArbeidssøkerClient = mockk()

        every { client.hentPerioder(any(), any(), any()) } answers {
            val startTidspunkt = secondArg<LocalDate>().minusMonths(1)
            val sluttTidspunkt = LocalDate.now().plusDays(1)
            listOf(ArbeidssøkerPeriode(UUID.randomUUID(), LocalDateWrapper(LocalDateTime.from(startTidspunkt)), LocalDateWrapper(LocalDateTime.from(sluttTidspunkt))))
        }

        return client
    }
}
