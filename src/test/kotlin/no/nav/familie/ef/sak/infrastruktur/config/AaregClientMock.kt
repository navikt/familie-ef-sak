package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsforhold.Ansettelsesperiode
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsavtaler
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsgiver
import no.nav.familie.kontrakter.felles.arbeidsforhold.ArbeidsgiverType
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidstaker
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class AaregClientMock {
    @Profile("mock-aareg")
    @Bean
    @Primary
    fun arbeidsforholdClient(): ArbeidsforholdClient {
        val mockk = mockk<ArbeidsforholdClient>()
        val mockResponse =
            listOf(
                Arbeidsforhold(
                    navArbeidsforholdId = 1L,
                    arbeidsforholdId = "1",
                    arbeidstaker = Arbeidstaker("type", "offentligIdent", "id"),
                    arbeidsgiver = Arbeidsgiver(ArbeidsgiverType.Organisasjon, "orgnummer", "offentligIdent"),
                    type = "type",
                    ansettelsesperiode = Ansettelsesperiode(),
                    arbeidsavtaler = listOf(Arbeidsavtaler()),
                ),
            )
        every { mockk.hentArbeidsforhold(any()) } returns Ressurs.success(mockResponse)
        return mockk
    }
}
