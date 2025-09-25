package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdClient
import no.nav.familie.kontrakter.felles.arbeidsforhold.Ansettelsesperiode
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidssted
import no.nav.familie.kontrakter.felles.arbeidsforhold.ArbeidsstedType
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidstaker
import no.nav.familie.kontrakter.felles.arbeidsforhold.Ident
import no.nav.familie.kontrakter.felles.arbeidsforhold.IdentType
import no.nav.familie.kontrakter.felles.arbeidsforhold.Kodeverksentitet
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

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
                    id = "V911050676R16054L0001",
                    arbeidstaker = Arbeidstaker(listOf(Ident(IdentType.FOLKEREGISTERIDENT, "01019999999", true))),
                    arbeidssted = Arbeidssted(ArbeidsstedType.Underenhet, listOf(Ident(IdentType.ORGANISASJONSNUMMER, "987654321"))),
                    type = Kodeverksentitet("ordinaertArbeidsforhold", "Ordinært arbeidsforhold"),
                    ansettelsesperiode = Ansettelsesperiode(LocalDate.now().minusYears(2).toString(), LocalDate.now().toString()),
                    ansettelsesdetaljer = null,
                ),
            )
        every { mockk.hentArbeidsforhold(any()) } returns mockResponse
        return mockk
    }
}
