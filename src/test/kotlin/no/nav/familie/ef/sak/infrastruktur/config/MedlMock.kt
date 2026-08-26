package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.MedlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.Medlemskapsunntak
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
class MedlMock {
    @Profile("mock-medl")
    @Bean
    @Primary
    fun medlClient(): MedlClient {
        val mockk = mockk<MedlClient>()
        val mockResponse =
            listOf(
                Medlemskapsunntak(
                    "Full",
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().plusMonths(3),
                    "FTL_2-5",
                    "01010199999",
                    true,
                    "GYLD",
                ),
            )
        every { mockk.hentMedlemskapsUnntak(any()) } returns mockResponse
        return mockk
    }
}
