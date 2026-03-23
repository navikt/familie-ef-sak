package no.nav.familie.ef.sak.simulering

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.kontrakter.felles.simulering.Simuleringsperiode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SimuleringUtilTest {
    private val januarStart = LocalDate.of(2021, 1, 1)
    private val aprilSlutt = LocalDate.of(2021, 4, 30)
    private val juniStart = LocalDate.of(2021, 6, 1)
    private val augustSlutt = LocalDate.of(2021, 8, 31)
    private val oktoberStart = LocalDate.of(2021, 10, 1)
    private val oktoberSlutt = LocalDate.of(2021, 10, 31)

    val simuleringsoppsummering =
        Simuleringsoppsummering(
            perioder =
                listOf(
                    lagSimuleringsperiode(YearMonth.of(2021, 1), nyttBeløp = -5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 2), nyttBeløp = -5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 3), nyttBeløp = -5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 4), nyttBeløp = -5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 5), nyttBeløp = 5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 6), nyttBeløp = -5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 7), nyttBeløp = -5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 8), nyttBeløp = -5000, tidligereUtbetalt = 0),
                    lagSimuleringsperiode(YearMonth.of(2021, 10), nyttBeløp = -5000, tidligereUtbetalt = 0),
                ),
            fomDatoNestePeriode = null,
            etterbetaling = BigDecimal.valueOf(5000),
            feilutbetaling = BigDecimal.valueOf(40_000),
            fom = LocalDate.of(2021, 1, 1),
            tomDatoNestePeriode = null,
            forfallsdatoNestePeriode = null,
            tidSimuleringHentet = LocalDate.of(2021, 11, 1),
            tomSisteUtbetaling = LocalDate.of(2021, 10, 31),
        )

    @Test
    internal fun `skal slå sammen perioder som har feilutbetalinger til sammenhengende perioder`() {
        println(jsonMapper.writeValueAsString(simuleringsoppsummering))

        val sammenhengendePerioderMedFeilutbetaling = simuleringsoppsummering.hentSammenhengendePerioderMedFeilutbetaling()
        Assertions.assertThat(sammenhengendePerioderMedFeilutbetaling).hasSize(3)
        Assertions.assertThat(sammenhengendePerioderMedFeilutbetaling.first().fomDato).isEqualTo(januarStart)
        Assertions.assertThat(sammenhengendePerioderMedFeilutbetaling.first().tomDato).isEqualTo(aprilSlutt)

        Assertions.assertThat(sammenhengendePerioderMedFeilutbetaling.second().fomDato).isEqualTo(juniStart)
        Assertions.assertThat(sammenhengendePerioderMedFeilutbetaling.second().tomDato).isEqualTo(augustSlutt)

        Assertions.assertThat(sammenhengendePerioderMedFeilutbetaling.last().fomDato).isEqualTo(oktoberStart)
        Assertions.assertThat(sammenhengendePerioderMedFeilutbetaling.last().tomDato).isEqualTo(oktoberSlutt)
    }

    fun lagSimuleringsperiode(
        mnd: YearMonth,
        nyttBeløp: Int,
        tidligereUtbetalt: Int,
    ): Simuleringsperiode {
        val resultat = nyttBeløp - tidligereUtbetalt

        return Simuleringsperiode(
            fom = mnd.atDay(1),
            tom = mnd.atEndOfMonth(),
            forfallsdato = mnd.atEndOfMonth(),
            nyttBeløp = nyttBeløp.toBigDecimal(),
            tidligereUtbetalt = tidligereUtbetalt.toBigDecimal(),
            resultat = resultat.toBigDecimal(),
            feilutbetaling = Integer.max(0 - resultat, 0).toBigDecimal(),
        )
    }

    private fun <E> List<E>.second(): E = this[1]
}
