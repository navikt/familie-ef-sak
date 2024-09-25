package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.ef.infotrygd.Vedtakstreff
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

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
                hentPerioderDefaultResponse(firstArg())
            }
            every { client.hentSammenslåttePerioder(any()) } answers {
                hentPerioderDefaultResponse(firstArg())
            }
            every { client.hentSaker(any()) } answers {
                InfotrygdSakResponse(
                    listOf(
                        InfotrygdSak(
                            personIdent = firstArg<InfotrygdSøkRequest>().personIdenter.first(),
                            stønadType = StønadType.BARNETILSYN,
                            resultat = InfotrygdSakResultat.ÅPEN_SAK,
                            type = InfotrygdSakType.KLAGE,
                        ),
                    ),
                )
            }
            every { client.hentInslagHosInfotrygd(any()) } answers {
                InfotrygdFinnesResponse(
                    emptyList(),
                    emptyList(),
                )
            }
            every { client.hentPersonerForMigrering(any()) } returns emptySet()
        }

        fun hentPerioderDefaultResponse(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse {
            val personIdent = request.personIdenter.first()
            return InfotrygdPeriodeResponse(
                overgangsstønad = listOf(lagInfotrygdPeriode(personIdent)),
                barnetilsyn =
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = personIdent,
                            beløp = 234,
                            inntektsgrunnlag = 321,
                            samordningsfradrag = 0,
                            utgifterBarnetilsyn = 1000,
                            barnIdenter = listOf("123", "234"),
                        ),
                    ),
                skolepenger = emptyList(),
            )
        }
    }
}
