package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.inntekt.ekstern.HentInntektListeResponse
import no.nav.familie.ef.sak.inntekt.ekstern.InntektClient
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class InntektClientMock {

    @Profile("mock-inntekt")
    @Bean
    @Primary
    fun inntektClient(): InntektClient {
        val mockk = mockk<InntektClient>()
        val mockResponseJson = this::class.java.classLoader.getResource("inntekt/inntektMock.json")!!
        val mockResponse = objectMapper.readValue<HentInntektListeResponse>(mockResponseJson)
        every { mockk.hentInntekt(any(), any(), any()) } returns mockResponse
        return mockk
    }

}
