package no.nav.familie.ef.sak.infrastruktur.config

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.amelding.ekstern.HentInntektListeResponse
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
    fun inntektClient(): AMeldingInntektClient {
        val mockk = mockk<AMeldingInntektClient>()
        val mockResponseJson = this::class.java.classLoader.getResource("inntekt/inntektMock.json")!!
        val mockResponse = objectMapper.readValue<HentInntektListeResponse>(mockResponseJson)
        every { mockk.hentInntekt(any(), any(), any()) } returns mockResponse

        val mockResponseUtenArbeidsinntektJson = this::class.java.classLoader.getResource("inntekt/inntektMockUtenArbeidsinntekt.json")!!
        val mockResponseUtenArbeidsinntekt = objectMapper.readValue<HentInntektListeResponse>(mockResponseUtenArbeidsinntektJson)
        every { mockk.hentInntekt("1", any(), any()) } returns mockResponseUtenArbeidsinntekt

        val mockResponseHøyArbeidsinntektJson = this::class.java.classLoader.getResource("inntekt/inntektMockMedHøyInntekt.json")!!
        val mockResponseHøyArbeidsinntekt = objectMapper.readValue<HentInntektListeResponse>(mockResponseHøyArbeidsinntektJson)
        every { mockk.hentInntekt("2", any(), any()) } returns mockResponseHøyArbeidsinntekt

        every { mockk.genererAInntektUrl(any()) } returns "https://ainntekt"
        return mockk
    }
}
