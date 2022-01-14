package no.nav.familie.ef.sak.infrastruktur.config

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
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
        clearMock()
        return iverksettClient
    }

    companion object {
        private val iverksettClient = mockk<IverksettClient>(relaxed = true)
        private val simuleringsresultat = objectMapper.readValue<BeriketSimuleringsresultat>(
                this::class.java.getResource("/json/simuleringsresultat_beriket.json")!!.readText())
        fun clearMock() {
            every { iverksettClient.simuler(any()) } returns simuleringsresultat
            every { iverksettClient.hentStatus(any()) } returns IverksettStatus.OK
        }
    }
}