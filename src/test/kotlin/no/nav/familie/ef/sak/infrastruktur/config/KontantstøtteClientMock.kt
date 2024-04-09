package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.kontantstøtte.HentUtbetalingsinfoKontantstøtte
import no.nav.familie.ef.sak.kontantstøtte.KontantstøtteClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-kontantstøtte")
class KontantstøtteClientMock {
    @Bean
    @Primary
    fun kontantstøtteClient(): KontantstøtteClient {
        val kontantstøtteClient: KontantstøtteClient = mockk()
        every { kontantstøtteClient.hentUtbetalingsinfo(any()) } returns
            HentUtbetalingsinfoKontantstøtte(
                emptyList(),
                emptyList(),
            )

        return kontantstøtteClient
    }
}
