package no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.math.BigDecimal

@Configuration
@Profile("mock-iverksett")
class IverksettClientMock {
    @Bean
    @Primary
    fun iverksettClient(): IverksettClient {
        val iverksettClient = mockk<IverksettClient>(relaxed = true)
        clearMock(iverksettClient)
        return iverksettClient
    }

    companion object {
        private val simuleringsresultat =
            jsonMapper.readValue<BeriketSimuleringsresultat>(
                this::class.java.getResource("/json/simuleringsresultat_beriket.json")!!.readText(),
            )

        fun clearMock(iverksettClient: IverksettClient) {
            clearMocks(iverksettClient)
            every { iverksettClient.simuler(any()) } returns simuleringsresultat
            every { iverksettClient.hentStatus(any()) } returns IverksettStatus.OK
        }

        fun mockSimulering(
            iverksettClient: IverksettClient,
            etterbetaling: Int = 0,
            feilutbetaling: Int = 0,
        ) {
            val oppsummering =
                Simuleringsoppsummering(
                    perioder = emptyList(),
                    fomDatoNestePeriode = null,
                    etterbetaling = BigDecimal(etterbetaling),
                    feilutbetaling = BigDecimal(feilutbetaling),
                    fom = null,
                    tomDatoNestePeriode = null,
                    forfallsdatoNestePeriode = null,
                    tidSimuleringHentet = null,
                    tomSisteUtbetaling = null,
                )
            every { iverksettClient.simuler(any()) } returns
                BeriketSimuleringsresultat(DetaljertSimuleringResultat(emptyList()), oppsummering)
        }
    }
}
