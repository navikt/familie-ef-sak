package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FullmaktDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt.FullmaktClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-fullmakt")
class FullmaktClientMock {
    @Bean
    @Primary
    fun fullmaktClient(): FullmaktClient {
        val fullmaktClient: FullmaktClient = mockk()
        every { fullmaktClient.hentFullmakt(any()) } returns
            FullmaktDto(
                LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(1),
                "1",
                "Navn",
                listOf("OMRÅDE"),
            )

        return fullmaktClient
    }
}
