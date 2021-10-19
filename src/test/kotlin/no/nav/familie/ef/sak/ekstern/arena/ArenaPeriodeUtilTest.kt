package no.nav.familie.ef.sak.ekstern.arena

import no.nav.familie.ef.sak.ekstern.arena.ArenaPeriodeUtil.mapOgFiltrer
import no.nav.familie.ef.sak.ekstern.arena.ArenaPeriodeUtil.slåSammenPerioder
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdArenaPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderArenaResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArenaPeriodeUtilTest {

    private val ident = "01234567890"

    @Test
    internal fun `skal slå sammen perioder til arena`() {
        val infotrygPerioder = listOf(periode(LocalDate.parse("2017-08-01"), LocalDate.parse("2018-04-30"), 1f,
                                              LocalDate.parse("2018-07-31")),
                                      periode(LocalDate.parse("2018-05-01"), LocalDate.parse("2020-07-31"), 1f,
                                              LocalDate.parse("2018-07-31")),
                                      periode(LocalDate.parse("2018-09-01"), LocalDate.parse("2018-12-31"), 1f),
                                      periode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-04-30"), 1f),
                                      periode(LocalDate.parse("2019-05-01"), LocalDate.parse("2020-04-30"), 1f),
                                      periode(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-08-31"), 1f))


        val perioder = slåSammenPerioder(mapOgFiltrer(InfotrygdPerioderArenaResponse(infotrygPerioder)))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(LocalDate.parse("2017-08-01") to LocalDate.parse("2018-07-31"),
                                  LocalDate.parse("2018-09-01") to LocalDate.parse("2020-08-31")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena 2`() {
        val infotrygPerioder = listOf(periode(LocalDate.parse("2018-12-01"), LocalDate.parse("2019-04-30"), 1f),
                                      periode(LocalDate.parse("2019-05-01"), LocalDate.parse("2019-12-31"), 1f),
                                      periode(LocalDate.parse("2019-12-01"), LocalDate.parse("2020-02-29"), 1f),
                                      periode(LocalDate.parse("2020-02-01"), LocalDate.parse("2020-04-30"), 1f),
                                      periode(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-10-31"), 1f),
                                      periode(LocalDate.parse("2020-09-01"), LocalDate.parse("2020-12-31"), 1f),
                                      periode(LocalDate.parse("2021-01-01"), LocalDate.parse("2022-01-31"), 1f))


        val perioder = slåSammenPerioder(mapOgFiltrer(InfotrygdPerioderArenaResponse(infotrygPerioder)))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(LocalDate.parse("2018-12-01") to LocalDate.parse("2022-01-31")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena 3`() {
        val infotrygPerioder = listOf(periode(LocalDate.parse("2017-06-01"),
                                              LocalDate.parse("2017-10-31"), 1f,
                                              LocalDate.parse("2018-07-31")),
                                      periode(LocalDate.parse("2017-08-01"),
                                              LocalDate.parse("2018-04-30"), 1f,
                                              LocalDate.parse("2018-07-31")),
                                      periode(LocalDate.parse("2018-05-01"),
                                              LocalDate.parse("2020-07-31"), 1f,
                                              LocalDate.parse("2018-07-31")),
                                      periode(LocalDate.parse("2018-09-01"), LocalDate.parse("2018-12-31"), 1f),
                                      periode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-04-30"), 1f),
                                      periode(LocalDate.parse("2019-05-01"), LocalDate.parse("2020-04-30"), 1f),
                                      periode(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-08-31"), 1f))


        val perioder = slåSammenPerioder(mapOgFiltrer(InfotrygdPerioderArenaResponse(infotrygPerioder)))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(
                        LocalDate.parse("2017-06-01") to LocalDate.parse("2018-07-31"),
                        LocalDate.parse("2018-09-01") to LocalDate.parse("2020-08-31")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena 4`() {
        val infotrygPerioder =
                listOf(periode(LocalDate.parse("2018-05-01"), LocalDate.parse("2018-05-31"), 1555f),
                       periode(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-05-31"), 2887f),
                       periode(LocalDate.parse("2018-03-01"), LocalDate.parse("2018-05-31"), 0f),
                       periode(LocalDate.parse("2018-02-01"), LocalDate.parse("2018-05-31"), 2024f),
                       periode(LocalDate.parse("2017-08-01"), LocalDate.parse("2018-05-31"), 3269f),
                       periode(LocalDate.parse("2016-09-01"), LocalDate.parse("2018-05-31"), 869f),
                       periode(LocalDate.parse("2016-04-01"), LocalDate.parse("2018-05-31"), 0f),
                       periode(LocalDate.parse("2016-03-01"), LocalDate.parse("2018-05-31"), 0f),
                       periode(LocalDate.parse("2018-01-01"), LocalDate.parse("2018-01-31"), 3299f),
                       periode(LocalDate.parse("2017-12-01"), LocalDate.parse("2017-12-31"), 0f),
                       periode(LocalDate.parse("2017-11-01"), LocalDate.parse("2017-12-31"), 1199f),
                       periode(LocalDate.parse("2017-10-01"), LocalDate.parse("2017-12-31"), 1612f),
                       periode(LocalDate.parse("2017-09-01"), LocalDate.parse("2017-12-31"), 0f),
                       periode(LocalDate.parse("2017-08-01"), LocalDate.parse("2017-12-31"), 1424f),
                       periode(LocalDate.parse("2017-02-01"), LocalDate.parse("2017-12-31"), 306f),
                       periode(LocalDate.parse("2017-05-01"), LocalDate.parse("2017-11-30"), 3269f),
                       periode(LocalDate.parse("2017-04-01"), LocalDate.parse("2017-04-30"), 2406f),
                       periode(LocalDate.parse("2017-03-01"), LocalDate.parse("2017-04-30"), 0f),
                       periode(LocalDate.parse("2017-02-01"), LocalDate.parse("2017-04-30"), 3006f),
                       periode(LocalDate.parse("2017-01-01"), LocalDate.parse("2017-02-28"), 2969f),
                       periode(LocalDate.parse("2016-12-01"), LocalDate.parse("2017-02-28"), 0f),
                       periode(LocalDate.parse("2016-11-01"), LocalDate.parse("2017-02-28"), 2256f),
                       periode(LocalDate.parse("2016-10-01"), LocalDate.parse("2017-02-28"), 2069f),
                       periode(LocalDate.parse("2016-09-01"), LocalDate.parse("2017-02-28"), 269f),
                       periode(LocalDate.parse("2016-08-01"), LocalDate.parse("2016-08-31"), 2481f),
                       periode(LocalDate.parse("2016-07-01"), LocalDate.parse("2016-07-31"), 3194f),
                       periode(LocalDate.parse("2016-06-01"), LocalDate.parse("2016-06-30"), 0f),
                       periode(LocalDate.parse("2016-05-01"), LocalDate.parse("2016-05-31"), 269f))

        val perioder = slåSammenPerioder(mapOgFiltrer(InfotrygdPerioderArenaResponse(infotrygPerioder)))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(
                        LocalDate.parse("2016-05-01") to LocalDate.parse("2016-05-31"),
                        LocalDate.parse("2016-07-01") to LocalDate.parse("2018-05-31"))
                )
    }

    @Test
    internal fun `skal slå sammen perioder til arena 5 - perioder som har samme startdato og sluttdato`() {
        val infotrygPerioder =
                listOf(periode(LocalDate.parse("2017-01-01"),
                               LocalDate.parse("2017-06-30"), 17358f,
                               LocalDate.parse("2017-01-01")),
                       periode(LocalDate.parse("2017-01-01"),
                               LocalDate.parse("2017-01-31"), 17358f,
                               LocalDate.parse("2017-01-01")),
                       periode(LocalDate.parse("2017-01-01"),
                               LocalDate.parse("2017-05-31"), 17358f,
                               LocalDate.parse("2017-03-31")))

        val perioder = slåSammenPerioder(mapOgFiltrer(InfotrygdPerioderArenaResponse(infotrygPerioder)))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(LocalDate.parse("2017-01-01") to LocalDate.parse("2017-03-31")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena - sammenhengende måneder`() {
        val infotrygPerioder =
                listOf(periode(LocalDate.parse("2011-08-01"), LocalDate.parse("2011-08-01"), 1f),
                       periode(LocalDate.parse("2011-09-01"), LocalDate.parse("2017-02-28"), 1f))


        val perioder = slåSammenPerioder(mapOgFiltrer(InfotrygdPerioderArenaResponse(infotrygPerioder)))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(LocalDate.parse("2011-08-01") to LocalDate.parse("2017-02-28")))
    }

    @Test
    internal fun `skal slå sammen perioder til arena - opphør før FOM`() {
        val infotrygPerioder =
                listOf(periode(LocalDate.parse("2013-04-01"), LocalDate.parse("2013-06-30"), 2013f),
                       periode(LocalDate.parse("2009-02-01"),
                               LocalDate.parse("2011-04-30"), 11534f,
                               LocalDate.parse("2009-01-31")),
                       periode(LocalDate.parse("2009-02-01"),
                               LocalDate.parse("2011-04-30"), 11534f,
                               LocalDate.parse("2009-02-28")),
                       periode(LocalDate.parse("2008-12-01"),
                               LocalDate.parse("2009-01-31"), 11534f,
                               LocalDate.parse("2009-01-31")))


        val perioder = slåSammenPerioder(mapOgFiltrer(InfotrygdPerioderArenaResponse(infotrygPerioder)))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(LocalDate.parse("2008-12-01") to LocalDate.parse("2009-02-28"),
                                  LocalDate.parse("2013-04-01") to LocalDate.parse("2013-06-30")))
    }

    @Test
    internal fun `skal slå sammen perioder fra ulike stønadstyper fra ef`() {
        val perioder = slåSammenPerioder(mapOgFiltrer(listOf(
                lagAndelTilkjentYtelse(1, LocalDate.parse("2021-01-01"), LocalDate.parse("2021-01-31")),
                lagAndelTilkjentYtelse(1, LocalDate.parse("2021-03-01"), LocalDate.parse("2021-05-31")),
                lagAndelTilkjentYtelse(1, LocalDate.parse("2021-04-01"), LocalDate.parse("2021-07-31")),
        )))

        assertThat(perioder.tilFomTomDato())
                .isEqualTo(listOf(LocalDate.parse("2021-01-01") to LocalDate.parse("2021-01-31"),
                                  LocalDate.parse("2021-03-01") to LocalDate.parse("2021-07-31")))
    }

    private fun List<PeriodeOvergangsstønad>.tilFomTomDato(): List<Pair<LocalDate, LocalDate>> =
            this.map { it.fomDato to it.tomDato }

    private fun periode(fomDato: LocalDate, tomDato: LocalDate, beløp: Float, opphørsdato: LocalDate? = null) =
            InfotrygdArenaPeriode(ident, fomDato, tomDato, beløp, opphørsdato)

}