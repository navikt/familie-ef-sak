package no.nav.familie.ef.sak.no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.simulering.erUnder4Rettsgebyr
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SimuleringoppsummeringTilDtoTest {
    @Test
    fun `Skal sjekke erUnder4Rettsgebyr uten å bruke scale på etterbetaling`() {
        val erUnder4Rettsgebyr = lagSimuleringsoppsummering(etterbetaling = BigDecimal("0.00"), feilutbetaling = BigDecimal("234")).erUnder4Rettsgebyr(345)
        Assertions.assertThat(erUnder4Rettsgebyr).isTrue
    }

    fun lagSimuleringsoppsummering(
        etterbetaling: BigDecimal = BigDecimal("0.00"),
        feilutbetaling: BigDecimal = BigDecimal("234"),
    ) = Simuleringsoppsummering(
        perioder = emptyList(),
        fomDatoNestePeriode = null,
        etterbetaling = etterbetaling,
        feilutbetaling = feilutbetaling,
        fom = null,
        tomDatoNestePeriode = null,
        forfallsdatoNestePeriode = null,
        tidSimuleringHentet = null,
        tomSisteUtbetaling = null,
        sumManuellePosteringer = null,
    )
}
