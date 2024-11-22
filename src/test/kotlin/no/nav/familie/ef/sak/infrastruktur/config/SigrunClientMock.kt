package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.sigrun.ekstern.BeregnetSkatt
import no.nav.familie.ef.sak.sigrun.ekstern.Grunnlag
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektForSkatteordning
import no.nav.familie.ef.sak.sigrun.ekstern.PensjonsgivendeInntektResponse
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.Skatteordning
import no.nav.familie.ef.sak.sigrun.ekstern.SummertSkattegrunnlag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate
import java.time.YearMonth

@Configuration
class SigrunClientMock {
    @Profile("mock-sigrun")
    @Bean
    @Primary
    fun sigrunClient(): SigrunClient {
        val mockk = mockk<SigrunClient>()
        val beregnetSkattMockResponse =
            listOf(
                BeregnetSkatt(
                    "tekniskNavn",
                    "verdi",
                ),
            )

        val summertSkattegrunnlagMockResponse =
            SummertSkattegrunnlag(
                listOf(Grunnlag("personinntektNaering", 400000)),
                listOf(Grunnlag("svalbardPersoninntektNaering", 350000)),
                LocalDate.of(2022, 12, 31).toString(),
            )

        val pensjonsgivendeInntektForSkatteordning = PensjonsgivendeInntektForSkatteordning(Skatteordning.FASTLAND, LocalDate.now(), 0, 0, 100_000, 0)
        val pensjonsgivendeInntektResponse = PensjonsgivendeInntektResponse("01010199999", YearMonth.now().minusYears(1).year, listOf(pensjonsgivendeInntektForSkatteordning))

        every { mockk.hentBeregnetSkatt(any(), any()) } returns beregnetSkattMockResponse
        every { mockk.hentSummertSkattegrunnlag(any(), any()) } returns summertSkattegrunnlagMockResponse
        every { mockk.hentPensjonsgivendeInntekt(any(), any()) } returns pensjonsgivendeInntektResponse

        return mockk
    }
}
