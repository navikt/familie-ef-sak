package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderArenaResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("mock-infotrygd-replika")
class InfotrygdReplikaMock {

    @Bean
    @Primary
    fun infotrygdReplikaClient(): InfotrygdReplikaClient {
        val client = mockk<InfotrygdReplikaClient>()
        every { client.hentPerioder(any()) } answers {
            val firstArg = firstArg<InfotrygdPeriodeRequest>()
            val personIdent = firstArg.personIdenter.first()
            InfotrygdPeriodeResponse(emptyList(), listOf(lagInfotrygdPeriode(personIdent)), emptyList())
        }
        every { client.hentPerioderArena(any()) } returns InfotrygdPerioderArenaResponse(emptyList())
        every { client.hentInslagHosInfotrygd(any()) } answers { InfotrygdFinnesResponse(emptyList(), emptyList()) }
        return client
    }

    private fun lagInfotrygdPeriode(personIdent: String) =
            InfotrygdPeriode(personIdent = personIdent,
                             kode = InfotrygdEndringKode.NY,
                             brukerId = "k000000",
                             stønadId = 1L,
                             vedtakId = 1L,
                             stønadBeløp = 100,
                             inntektsreduksjon = 10,
                             samordningsfradrag = 20,
                             beløp = 10,
                             startDato = LocalDate.of(2021, 1, 1),
                             stønadFom = LocalDate.of(2021, 1, 1),
                             stønadTom = LocalDate.of(2021, 1, 31),
                             opphørsdato = null)

}