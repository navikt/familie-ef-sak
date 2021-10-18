package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.simulering.SimuleringsposteringTestUtil.lagPosteringer
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal class SimuleringUtilTest {

    @Test
    internal fun `skal ikke mappe simuleringsdata for forskuddskatt, motp, justering og trekk `() {
        val fraDato = LocalDate.of(2020, 1, 1)
        val simuleringsmottakere = listOf(SimuleringMottaker(
                simulertPostering = lagPosteringer(fraDato, posteringstype = PosteringType.MOTP)
                                    + lagPosteringer(fraDato, posteringstype = PosteringType.FORSKUDSSKATT)
                                    + lagPosteringer(fraDato, posteringstype = PosteringType.JUSTERING)
                                    + lagPosteringer(fraDato, posteringstype = PosteringType.TREKK),
                mottakerNummer = "12345678901",
                mottakerType = MottakerType.BRUKER
        ))

        val simuleringsresultatDto =
                tilSimuleringsoppsummering(DetaljertSimuleringResultat(simuleringsmottakere), fraDato.plusMonths(12))

        assertThat(simuleringsresultatDto.perioder).isEmpty()
        assertThat(simuleringsresultatDto.etterbetaling).isZero()
        assertThat(simuleringsresultatDto.feilutbetaling).isZero()
    }

    @Test
    internal fun `skal mappe simuleringsdata for enkel ytelse`() {
        val fraDato = LocalDate.of(2020, 1, 1)
        val beløp = BigDecimal(5000)
        val antallMåneder = 36
        val simuleringsmottakere = listOf(SimuleringMottaker(
                simulertPostering = lagPosteringer(fraDato,
                                                   posteringstype = PosteringType.YTELSE,
                                                   antallMåneder = antallMåneder,
                                                   beløp = beløp),
                mottakerNummer = "12345678901",
                mottakerType = MottakerType.BRUKER
        ))

        val antallMånederEtterStart: Long = 12
        val tidSimuleringHentet = fraDato.plusMonths(antallMånederEtterStart)
        val simuleringsresultatDto =
                tilSimuleringsoppsummering(DetaljertSimuleringResultat(simuleringsmottakere), tidSimuleringHentet)

        val posteringerGruppert = simuleringsresultatDto.perioder
        assertThat(posteringerGruppert).hasSize(antallMåneder)
        assertThat(posteringerGruppert.sumOf { it.feilutbetaling }).isZero
        assertThat(posteringerGruppert.sumOf { it.resultat }).isEqualTo(beløp.multiply(BigDecimal(antallMåneder)))
        assertThat(posteringerGruppert.first().nyttBeløp).isEqualTo(beløp)
        assertThat(posteringerGruppert.last().nyttBeløp).isEqualTo(beløp)
        assertThat(posteringerGruppert.first().fom).isEqualTo(fraDato)
        assertThat(posteringerGruppert.last().fom).isEqualTo(fraDato.plusMonths(antallMåneder.toLong() - 1))
        assertThat(simuleringsresultatDto.etterbetaling).isEqualTo(beløp.multiply(antallMånederEtterStart.toBigDecimal()))
        assertThat(simuleringsresultatDto.feilutbetaling).isZero
        assertThat(simuleringsresultatDto.fom).isEqualTo(fraDato)
        assertThat(simuleringsresultatDto.forfallsdatoNestePeriode)
                .isEqualTo(tidSimuleringHentet.with(TemporalAdjusters.lastDayOfMonth()))
    }


    @Test
    internal fun `skal mappe simuleringsdata for ytelse hvor bruker har fått for mye i 6 måneder`() {
        val fraDato = LocalDate.of(2020, 1, 1)
        val antallMåneder = 12
        val antallMånederFeilutbetalt = 6
        val fraDatoFeilutbetalt = fraDato.plusMonths(antallMånederFeilutbetalt.toLong())
        val beløp = BigDecimal(5000)
        val nyttBeløp = BigDecimal(3000)
        val simuleringsmottakere = listOf(SimuleringMottaker(
                simulertPostering = lagPosteringer(fraDato,
                                                   posteringstype = PosteringType.YTELSE,
                                                   antallMåneder = antallMåneder - antallMånederFeilutbetalt,
                                                   beløp = beløp)
                                    + lagPosteringer(fraDatoFeilutbetalt,
                                                     posteringstype = PosteringType.FEILUTBETALING,
                                                     antallMåneder = antallMånederFeilutbetalt,
                                                     beløp = beløp.minus(nyttBeløp))
                                    + lagPosteringer(fraDatoFeilutbetalt,
                                                     posteringstype = PosteringType.YTELSE,
                                                     antallMåneder = antallMånederFeilutbetalt,
                                                     beløp = beløp.negate())
                                    + lagPosteringer(fraDatoFeilutbetalt,
                                                     posteringstype = PosteringType.YTELSE,
                                                     antallMåneder = antallMånederFeilutbetalt + 1,
                                                     beløp = nyttBeløp)
                                    + lagPosteringer(fraDatoFeilutbetalt,
                                                     posteringstype = PosteringType.YTELSE,
                                                     antallMåneder = antallMånederFeilutbetalt,
                                                     beløp = beløp.minus(nyttBeløp)),
                mottakerNummer = "12345678901",
                mottakerType = MottakerType.BRUKER
        ))

        val antallMånederEtterStart: Long = 12
        val tidSimuleringHentet = fraDato.plusMonths(antallMånederEtterStart)
        val simuleringsresultatDto =
                tilSimuleringsoppsummering(DetaljertSimuleringResultat(simuleringsmottakere), tidSimuleringHentet)

        val posteringerGruppert = simuleringsresultatDto.perioder
        val totaltFeilutbetaltBeløp = beløp.minus(nyttBeløp).multiply(BigDecimal(antallMånederFeilutbetalt))

        assertThat(posteringerGruppert).hasSize(antallMåneder + 1)
        assertThat(posteringerGruppert.sumOf { it.feilutbetaling }).isEqualTo(totaltFeilutbetaltBeløp)
        assertThat(posteringerGruppert.sumOf { it.nyttBeløp }).isEqualTo(nyttBeløp.plus(beløp.multiply(BigDecimal(antallMåneder))
                                                                                                .minus(totaltFeilutbetaltBeløp)))
        assertThat(posteringerGruppert.sumOf { it.resultat }).isEqualTo(nyttBeløp.plus(
                beløp.multiply(BigDecimal(antallMåneder - antallMånederFeilutbetalt)).minus(totaltFeilutbetaltBeløp))
        )
        assertThat(posteringerGruppert.first().nyttBeløp).isEqualTo(beløp)
        assertThat(posteringerGruppert.last().nyttBeløp).isEqualTo(nyttBeløp)
        assertThat(posteringerGruppert.first().fom).isEqualTo(fraDato)
        assertThat(posteringerGruppert.last().fom).isEqualTo(fraDato.plusMonths(antallMåneder.toLong()))
        assertThat(simuleringsresultatDto.etterbetaling)
                .isEqualTo(beløp.multiply(BigDecimal(antallMånederEtterStart - antallMånederFeilutbetalt)))
        assertThat(simuleringsresultatDto.feilutbetaling).isEqualTo(totaltFeilutbetaltBeløp)
        assertThat(simuleringsresultatDto.fom).isEqualTo(fraDato)
        assertThat(simuleringsresultatDto.forfallsdatoNestePeriode)
                .isEqualTo(tidSimuleringHentet.with(TemporalAdjusters.lastDayOfMonth()))
    }

}