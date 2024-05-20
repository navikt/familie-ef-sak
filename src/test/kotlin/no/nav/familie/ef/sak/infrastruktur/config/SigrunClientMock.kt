package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.sigrun.ekstern.BeregnetSkatt
import no.nav.familie.ef.sak.sigrun.ekstern.Grunnlag
import no.nav.familie.ef.sak.sigrun.ekstern.SigrunClient
import no.nav.familie.ef.sak.sigrun.ekstern.SummertSkattegrunnlag
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

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

        every { mockk.hentBeregnetSkatt(any(), any()) } returns beregnetSkattMockResponse
        every { mockk.hentSummertSkattegrunnlag(any(), any()) } returns summertSkattegrunnlagMockResponse

        return mockk
    }
}
