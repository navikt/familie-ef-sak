package no.nav.familie.ef.sak.infrastruktur.config

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.repository.inntekt
import no.nav.familie.ef.sak.repository.inntektsmåneder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.YearMonth

@Configuration
class InntektClientMock {
    @Profile("mock-inntekt")
    @Bean
    @Primary
    fun inntektClient(): AMeldingInntektClient {
        val mockk = mockk<AMeldingInntektClient>()
        val mockResponseJson = this::class.java.classLoader.getResource("json/inntekt/InntektMock.json")!!
        val mockResponse = objectMapper.readValue<InntektResponse>(mockResponseJson)
        every { mockk.hentInntekt(any(), any(), any()) } returns mockResponse

        val mockResponseUtenArbeidsinntektJson = this::class.java.classLoader.getResource("json/inntekt/InntektMockUtenArbeidsinntekt.json")!!
        val mockResponseUtenArbeidsinntekt = objectMapper.readValue<InntektResponse>(mockResponseUtenArbeidsinntektJson)
        every { mockk.hentInntekt("1", any(), any()) } returns mockResponseUtenArbeidsinntekt

        val mockResponseHøyArbeidsinntektJson = this::class.java.classLoader.getResource("json/inntekt/InntektMockMedHøyInntekt.json")!!
        val mockResponseHøyArbeidsinntekt = objectMapper.readValue<InntektResponse>(mockResponseHøyArbeidsinntektJson)
        every { mockk.hentInntekt("2", any(), any()) } returns mockResponseHøyArbeidsinntekt

        val inntekterPerMånedLavInntekt = listOf(inntekt(2000.0), inntekt(10000.0))
        val inntektsmånederLavInntekt = inntektsmåneder(fraOgMedMåned = YearMonth.now().minusMonths(12), tilOgMedMåned = YearMonth.now().minusMonths(4), inntektListe = inntekterPerMånedLavInntekt)
        val inntekterPerMånedHøyInntekt = listOf(inntekt(25000.0), inntekt(10000.0))
        val inntektsmånederHøyInntekt = inntektsmåneder(fraOgMedMåned = YearMonth.now().minusMonths(3), inntektListe = inntekterPerMånedHøyInntekt)
        every { mockk.hentInntekt("3", any(), any()) } returns InntektResponse(inntektsmånederLavInntekt + inntektsmånederHøyInntekt)

        return mockk
    }
}
