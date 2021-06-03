package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.time.LocalDate

@Configuration
@Profile("mock-iverksett")
class IverksettClientMock {

    @Bean
    @Primary
    fun iverksettClient(): IverksettClient {
        val iverksettClient = mockk<IverksettClient>()

        val simulertPostering = SimulertPostering(fagOmrådeKode = FagOmrådeKode.ENSLIG_FORSØRGER,
                                                  fom = LocalDate.of(2021, 1, 1),
                                                  tom = LocalDate.of(2021, 12, 31),
                                                  betalingType = BetalingType.DEBIT,
                                                  beløp = BigDecimal.valueOf(15000L),
                                                  posteringType = PosteringType.YTELSE,
                                                  forfallsdato = LocalDate.of(2021, 1, 15),
                                                  utenInntrekk = false)

        every { iverksettClient.simuler(any()) } returns DetaljertSimuleringResultat(simuleringMottaker = listOf(
                SimuleringMottaker(simulertPostering = listOf(simulertPostering),
                                   mottakerNummer = "123",
                                   mottakerType = MottakerType.BRUKER)))

        return iverksettClient
    }
}