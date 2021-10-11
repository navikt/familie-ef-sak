package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.simulering.SimuleringsposteringTestUtil
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class SimuleringsresultatDtoTest{


    private val januarStart = LocalDate.of(2021, 1, 1)
    private val aprilSlutt = LocalDate.of(2021, 4, 30)
    private val juniStart = LocalDate.of(2021, 6, 1)
    private val augustSlutt = LocalDate.of(2021, 8, 31)
    private val oktoberStart = LocalDate.of(2021, 10, 1)
    private val oktoberSlutt = LocalDate.of(2021, 10, 31)
    val januarTilApril = SimuleringsposteringTestUtil.lagPosteringer(fraDato = januarStart,
                                                                     antallMåneder = 4,
                                                                     beløp = BigDecimal(5000),
                                                                     posteringstype = PosteringType.FEILUTBETALING)


    val mai = SimuleringsposteringTestUtil.lagPosteringer(fraDato = LocalDate.of(2021, 5, 1),
                                                                  antallMåneder = 1,
                                                                  beløp = BigDecimal(5000),
                                                                  posteringstype = PosteringType.YTELSE)


    val juniTilAugust = SimuleringsposteringTestUtil.lagPosteringer(fraDato = juniStart,
                                                                  antallMåneder = 3,
                                                                  beløp = BigDecimal(5000),
                                                                  posteringstype = PosteringType.FEILUTBETALING)

    val oktober = SimuleringsposteringTestUtil.lagPosteringer(fraDato = oktoberStart,
                                                                  antallMåneder = 1,
                                                                  beløp = BigDecimal(5000),
                                                                  posteringstype = PosteringType.FEILUTBETALING)


    val simuleringsmottakere = listOf(SimuleringMottaker(
            simulertPostering = januarTilApril + mai + juniTilAugust + oktober,
            mottakerNummer = "12345678901",
            mottakerType = MottakerType.BRUKER
    ))

    @Test
    internal fun `skal slå sammen perioder som har feilutbetalinger til sammenhengende perioder`() {
        val simuleringsresultatDto =
                tilSimuleringsresultatDto(DetaljertSimuleringResultat(simuleringsmottakere), LocalDate.of(2021, 11, 1))

        val sammenhengendePerioderMedFeilutbetaling = simuleringsresultatDto.hentSammenhengendePerioderMedFeilutbetaling()
        assertThat(sammenhengendePerioderMedFeilutbetaling).hasSize(3)
        assertThat(sammenhengendePerioderMedFeilutbetaling.first().fom).isEqualTo(januarStart)
        assertThat(sammenhengendePerioderMedFeilutbetaling.first().tom).isEqualTo(aprilSlutt)

        assertThat(sammenhengendePerioderMedFeilutbetaling.second().fom).isEqualTo(juniStart)
        assertThat(sammenhengendePerioderMedFeilutbetaling.second().tom).isEqualTo(augustSlutt)

        assertThat(sammenhengendePerioderMedFeilutbetaling.last().fom).isEqualTo(oktoberStart)
        assertThat(sammenhengendePerioderMedFeilutbetaling.last().tom).isEqualTo(oktoberSlutt)

    }
}

private fun <E> List<E>.second(): E {
    return this[1]
}
