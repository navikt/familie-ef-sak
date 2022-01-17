package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderArenaResponse
import no.nav.familie.kontrakter.ef.infotrygd.Saktreff
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
        every { client.hentInslagHosInfotrygd(any()) } answers {
            InfotrygdFinnesResponse(emptyList(), listOf(Saktreff("", StønadType.OVERGANGSSTØNAD)))
        }
        return client
    }

    private fun lagInfotrygdPeriode(personIdent: String) =
            lagInfotrygdPeriode(personIdent = personIdent,
                                kode = InfotrygdEndringKode.NY,
                                inntektsreduksjon = 10,
                                samordningsfradrag = 20,
                                beløp = 10,
                                stønadFom = LocalDate.of(2021, 1, 1),
                                stønadTom = LocalDate.of(2021, 1, 31),
                                opphørsdato = null)

}