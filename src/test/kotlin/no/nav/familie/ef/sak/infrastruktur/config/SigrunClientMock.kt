package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektForSkatteordning
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektResponse
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.Skatteordning
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
class SigrunClientMock {
    @Profile("mock-sigrun")
    @Bean
    @Primary
    fun sigrunClient(): SigrunClient {
        val mockk = mockk<SigrunClient>()

        val pensjonsgivendeInntektMockResponse =
            PensjonsgivendeInntektResponse(
                norskPersonidentifikator = "12345678901",
                inntektsaar = 2022,
                pensjonsgivendeInntekt =
                    listOf(
                        PensjonsgivendeInntektForSkatteordning(
                            skatteordning = Skatteordning.FASTLAND,
                            datoForFastsetting = LocalDate.of(2023, 9, 27),
                            pensjonsgivendeInntektAvLoennsinntekt = 500000,
                            pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = null,
                            pensjonsgivendeInntektAvNaeringsinntekt = 150000,
                            pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = null,
                        ),
                    ),
            )

        every { mockk.hentPensjonsgivendeInntekt(any(), any()) } returns pensjonsgivendeInntektMockResponse

        return mockk
    }
}
