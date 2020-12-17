package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.dummy.KonsistensavstemmingRequestV2
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-oppdrag")
class OppdragClientMock {

    @Bean
    @Primary
    fun oppdragClient(): OppdragClient {
        val oppdragClientMock = mockk<OppdragClient>()

        every { oppdragClientMock.konsistensavstemming(any<KonsistensavstemmingRequest>()) } returns "OK"
        every { oppdragClientMock.konsistensavstemming(any<KonsistensavstemmingRequestV2>()) } returns "OK"
        every { oppdragClientMock.grensesnittavstemming(any()) } returns "OK"
        every { oppdragClientMock.iverksettOppdrag(any()) } returns "OK"
        every { oppdragClientMock.hentStatus(any()) } returns OppdragStatus.KVITTERT_OK

        return oppdragClientMock
    }
}