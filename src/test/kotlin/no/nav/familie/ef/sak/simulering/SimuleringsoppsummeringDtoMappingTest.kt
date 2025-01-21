package no.nav.familie.ef.sak.no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.simulering.rettsgebyrForÅr
import no.nav.familie.ef.sak.simulering.tilSimuleringsoppsummeringDto
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.kontrakter.felles.simulering.Simuleringsperiode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate


class SimuleringsoppsummeringDtoMappingTest {

    @Test
    fun `har gyldig rettsgebyr for gjeldene år (klager først etter 20 dager)`() {
        Assertions.assertThat(rettsgebyrForÅr[LocalDate.now().minusDays(20).year]).isNotNull()
    }

    @Test
    fun `skal mappe Simuleringsoppsummering til SimuleringsoppsummeringDto`() {
        val simuleringsoppsummering = lagSimuleringsoppsummering()

        val simuleringsoppsummeringDto = simuleringsoppsummering.tilSimuleringsoppsummeringDto()
        Assertions.assertThat(simuleringsoppsummeringDto.perioder).hasSize(1)
        Assertions.assertThat(simuleringsoppsummeringDto.visUnder4rettsgebyr).isFalse()
        Assertions.assertThat(simuleringsoppsummeringDto.feilutbetalingsår).isNull()
        Assertions.assertThat(simuleringsoppsummeringDto.fireRettsgebyr).isNull()
    }

    @Test
    fun `skal mappe Simuleringsoppsummering med feilutbetaling under 4 rettsgebyr 2021`() {
        val simuleringsoppsummering = lagSimuleringsoppsummering(perioder = listOf(lagSimuleringsperiode(feilutbetaling = BigDecimal.valueOf(1000))))

        val simuleringsoppsummeringDto = simuleringsoppsummering.tilSimuleringsoppsummeringDto()
        Assertions.assertThat(simuleringsoppsummeringDto.perioder).hasSize(1)
        Assertions.assertThat(simuleringsoppsummeringDto.visUnder4rettsgebyr).isTrue()
        Assertions.assertThat(simuleringsoppsummeringDto.feilutbetalingsår).isEqualTo(2021)
        Assertions.assertThat(simuleringsoppsummeringDto.fireRettsgebyr).isEqualTo(rettsgebyrForÅr[2021]!! * 4)
    }

    @Test
    fun `skal mappe Simuleringsoppsummering med feilutbetaling under 4 rettsgebyr 2022`() {
        val simuleringsoppsummering = lagSimuleringsoppsummering(perioder = listOf(lagSimuleringsperiode(tom = LocalDate.of(2022, 1, 1), feilutbetaling = BigDecimal.valueOf(1000))))

        val simuleringsoppsummeringDto = simuleringsoppsummering.tilSimuleringsoppsummeringDto()
        Assertions.assertThat(simuleringsoppsummeringDto.perioder).hasSize(1)
        Assertions.assertThat(simuleringsoppsummeringDto.visUnder4rettsgebyr).isTrue()
        Assertions.assertThat(simuleringsoppsummeringDto.feilutbetalingsår).isEqualTo(2022)
        Assertions.assertThat(simuleringsoppsummeringDto.fireRettsgebyr).isEqualTo(rettsgebyrForÅr[2022]!! * 4)
    }

    @Test
    fun `skal mappe Simuleringsoppsummering med feilutbetaling under 4 rettsgebyr 2022 med etterbetaling`() {
        val simuleringsoppsummering = lagSimuleringsoppsummering(
            perioder = listOf(lagSimuleringsperiode(tom = LocalDate.of(2022, 1, 1), feilutbetaling = BigDecimal.valueOf(1000))),
            etterbetaling = 1000,
        )

        val simuleringsoppsummeringDto = simuleringsoppsummering.tilSimuleringsoppsummeringDto()
        Assertions.assertThat(simuleringsoppsummeringDto.perioder).hasSize(1)
        Assertions.assertThat(simuleringsoppsummeringDto.visUnder4rettsgebyr).isFalse()
    }
    @Test
    fun `skal mappe Simuleringsoppsummering med feilutbetaling over 4 rettsgebyr 2022`() {
        val simuleringsoppsummering = lagSimuleringsoppsummering(
            perioder = listOf(lagSimuleringsperiode(tom = LocalDate.of(2022, 1, 1), feilutbetaling = BigDecimal.valueOf(1000))),
            feilutbetaling = 10000,
        )

        val simuleringsoppsummeringDto = simuleringsoppsummering.tilSimuleringsoppsummeringDto()
        Assertions.assertThat(simuleringsoppsummeringDto.perioder).hasSize(1)
        Assertions.assertThat(simuleringsoppsummeringDto.visUnder4rettsgebyr).isFalse()
    }

    @Test
    fun `skal mappe Simuleringsoppsummering og bruke siste periode med feilutbetaling selv om de er usortert`() {
        val perioder = listOf(
            lagSimuleringsperiode(tom = LocalDate.of(2021, 1, 1), feilutbetaling = BigDecimal.valueOf(1000)),
            lagSimuleringsperiode(tom = LocalDate.of(2022, 1, 1), feilutbetaling = BigDecimal.valueOf(1000)),
            lagSimuleringsperiode(tom = LocalDate.of(2023, 1, 1), feilutbetaling = BigDecimal.valueOf(0)),
            lagSimuleringsperiode(tom = LocalDate.of(2021, 2, 1), feilutbetaling = BigDecimal.valueOf(1000)),
        )
        val simuleringsoppsummering = lagSimuleringsoppsummering(perioder = perioder)

        val simuleringsoppsummeringDto = simuleringsoppsummering.tilSimuleringsoppsummeringDto()
        Assertions.assertThat(simuleringsoppsummeringDto.perioder).hasSize(4)
        Assertions.assertThat(simuleringsoppsummeringDto.visUnder4rettsgebyr).isTrue()
        Assertions.assertThat(simuleringsoppsummeringDto.feilutbetalingsår).isEqualTo(2022)
        Assertions.assertThat(simuleringsoppsummeringDto.fireRettsgebyr).isEqualTo(rettsgebyrForÅr[2022]!! * 4)
    }

    private fun lagSimuleringsperiode(
        tom: LocalDate = LocalDate.of(2021, 1, 31),
        feilutbetaling: BigDecimal = BigDecimal.valueOf(0)
    ) = Simuleringsperiode(
        fom = LocalDate.of(2021, 1, 1),
        tom = tom,
        forfallsdato = LocalDate.of(2021, 1, 31),
        nyttBeløp = BigDecimal.valueOf(1000),
        tidligereUtbetalt = BigDecimal.valueOf(0),
        resultat = BigDecimal.valueOf(1000),
        feilutbetaling = feilutbetaling,
    )

    private fun lagSimuleringsoppsummering(
        perioder: List<Simuleringsperiode> = listOf(
            lagSimuleringsperiode()
        ),
        feilutbetaling: Long = 0,
        etterbetaling: Long = 0
    ) = Simuleringsoppsummering(
        perioder = perioder,
        fomDatoNestePeriode = LocalDate.of(2021, 2, 1),
        etterbetaling = BigDecimal.valueOf(etterbetaling),
        feilutbetaling = BigDecimal.valueOf(feilutbetaling),
        fom = LocalDate.of(2021, 1, 1),
        tomDatoNestePeriode = LocalDate.of(2021, 1, 31),
        forfallsdatoNestePeriode = LocalDate.of(2021, 1, 31),
        tidSimuleringHentet = LocalDate.of(2021, 1, 1),
        tomSisteUtbetaling = LocalDate.of(2021, 1, 31),
        sumManuellePosteringer = BigDecimal.valueOf(0),
        sumKreditorPosteringer = BigDecimal.valueOf(0),
    )
}