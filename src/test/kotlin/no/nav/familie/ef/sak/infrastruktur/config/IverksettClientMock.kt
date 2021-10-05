package no.nav.familie.ef.sak.config

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-iverksett")
class IverksettClientMock {

    @Bean
    @Primary
    fun iverksettClient(): IverksettClient {
        val iverksettClient = mockk<IverksettClient>()

        every { iverksettClient.simuler(any()) } returns objectMapper.readValue(readFile("simuleringsresultat.json"))

        every { iverksettClient.iverksett(any(), any()) } just Runs
        every { iverksettClient.iverksettTekniskOpphør(any()) } just Runs
        every { iverksettClient.hentStatus(any()) } returns IverksettStatus.OK
        every { iverksettClient.sendBehandlingsstatistikk(any()) } just Runs
        every { iverksettClient.sendFrittståendeBrev(any()) } just Runs

        return iverksettClient
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/json/$filnavn").readText()
    }
}