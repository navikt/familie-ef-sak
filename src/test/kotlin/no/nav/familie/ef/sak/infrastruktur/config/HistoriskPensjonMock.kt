package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class HistoriskPensjonMock {
    @Profile("mock-historiskpensjon")
    @Bean
    @Primary
    fun historiskPensjonClient(): HistoriskPensjonClient {
        val mockk = mockk<HistoriskPensjonClient>()
        every { mockk.hentHistoriskPensjonStatusForIdent(any(), any()) } returns HistoriskPensjonDto(HistoriskPensjonStatus.HAR_HISTORIKK, "")
        return mockk
    }
}
