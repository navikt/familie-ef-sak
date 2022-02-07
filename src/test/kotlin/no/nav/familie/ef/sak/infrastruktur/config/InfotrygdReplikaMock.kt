package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdEndringKode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderArenaResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
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
        resetMock(client)
        return client
    }

    companion object {

        fun resetMock(client: InfotrygdReplikaClient) {
            clearMocks(client)
            every { client.hentPerioder(any()) } answers {
                val firstArg = firstArg<InfotrygdPeriodeRequest>()
                val personIdent = firstArg.personIdenter.first()
                InfotrygdPeriodeResponse(
                        listOf(lagInfotrygdPeriode(kode = InfotrygdEndringKode.OPPHØRT, stønadTom = LocalDate.of(2022,3,1), opphørsdato = LocalDate.of(2022,1,1)),
                               lagInfotrygdPeriode(kode = InfotrygdEndringKode.FØRSTEGANGSVEDTAK, stønadTom = LocalDate.of(2022,3,1)),
                               lagInfotrygdPeriode(kode = InfotrygdEndringKode.FØRSTEGANGSVEDTAK, stønadTom = LocalDate.of(2021,3,31),
                                                   stønadFom = LocalDate.of(2021,3,1), vedtakId = 123, stønadId = 2),
                               lagInfotrygdPeriode(kode = InfotrygdEndringKode.OVERTFØRT_NY_LØSNING, stønadTom = LocalDate.of(2021,3,31),
                                                   stønadFom = LocalDate.of(2021,3,1), vedtakId = 124, stønadId = 2)),
                        listOf(lagInfotrygdPeriode(personIdent)),
                        emptyList()
                )
            }
            every { client.hentSaker(any()) } returns InfotrygdSakResponse(emptyList())
            every { client.hentInslagHosInfotrygd(any()) } answers { InfotrygdFinnesResponse(emptyList(), emptyList()) }
        }
    }

}