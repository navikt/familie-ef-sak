package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.felles.kodeverk.KodeverkService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-kodeverk")
class KodeverkServiceMock {
    @Bean
    @Primary
    fun kodeverkService(): KodeverkService {
        val kodeverkService = mockk<KodeverkService>()
        every { kodeverkService.hentLand("NOR", any()) } returns "Norge"
        every { kodeverkService.hentLand("SWE", any()) } returns "Sverige"
        every { kodeverkService.hentLand("POL", any()) } returns "Polen"
        every { kodeverkService.hentLand("ESP", any()) } returns "Spania"
        every { kodeverkService.hentPoststed(any(), any()) } returns "Oslo"
        return kodeverkService
    }
}
