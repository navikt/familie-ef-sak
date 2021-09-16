package no.nav.familie.ef.sak.no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.beregning.Beløpsperiode
import no.nav.familie.ef.sak.beregning.BeregningUtils.Companion.finnStartDatoOgSluttDatoForBeløpsperiode
import no.nav.familie.ef.sak.util.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BeregningUtilsTest {

    @Test
    internal fun `hvis vedtaksperiode omsluttes av beløpsperiode skal datoerne for vedtaksperiode returneres `() {
        val beløpsperiode =
                Beløpsperiode(periode = Periode(fradato = LocalDate.parse("2020-05-01"), tildato = LocalDate.parse("2020-12-01")),
                              beløp = 10_000.toBigDecimal(),
                              beløpFørSamordning = 12_000.toBigDecimal())
        val vedtaksperiode = Periode(fradato = LocalDate.parse("2020-07-01"), tildato = LocalDate.parse("2020-10-31"))
        assertThat(finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder = listOf(beløpsperiode),
                                                            vedtaksperiode = vedtaksperiode).first())
                .isEqualTo(beløpsperiode.copy(periode = vedtaksperiode))
    }

    @Test
    internal fun `hvis beløpsperiode omsluttes av vedtaksperiode skal datoerne for beløpsperiode være uforandrede`() {
        val beløpsperiode =
                Beløpsperiode(periode = Periode(fradato = LocalDate.parse("2020-07-01"), tildato = LocalDate.parse("2020-09-30")),
                              beløp = 10_000.toBigDecimal(),
                              beløpFørSamordning = 12_000.toBigDecimal())
        val vedtaksperiode = Periode(fradato = LocalDate.parse("2020-05-01"), tildato = LocalDate.parse("2020-12-31"))
        assertThat(finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder = listOf(beløpsperiode),
                                                            vedtaksperiode = vedtaksperiode).first())
                .isEqualTo(beløpsperiode)
    }

    @Test
    internal fun `hvis beløpsperiode overlapper i starten av vedtaksperiode skal startdatoen for vedtaksperiode returneres sammen med sluttdato for beløpsperiode`() {
        val beløpsperiode =
                Beløpsperiode(periode = Periode(fradato = LocalDate.parse("2020-03-01"),
                                                tildato = LocalDate.parse("2020-06-30")),
                              beløp = 10_000.toBigDecimal(),
                              beløpFørSamordning = 12_000.toBigDecimal())
        val vedtaksperiode = Periode(fradato = LocalDate.parse("2020-05-01"),
                                     tildato = LocalDate.parse("2020-12-31"))
        assertThat(finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder = listOf(beløpsperiode),
                                                            vedtaksperiode = vedtaksperiode).first())
                .isEqualTo(beløpsperiode.copy(periode = vedtaksperiode.copy(fradato = LocalDate.parse("2020-05-01"),
                                                                            tildato = LocalDate.parse("2020-06-30"))))
    }

    @Test
    internal fun `hvis beløpsperiode overlapper i slutten av vedtaksperiode skal startdatoen for beløpsperiode returneres sammen med sluttdato for vedtaksperiode`() {
        val beløpsperiode =
                Beløpsperiode(periode = Periode(fradato = LocalDate.parse("2020-09-01"),
                                                tildato = LocalDate.parse("2021-02-28")),
                              beløp = 10_000.toBigDecimal(),
                              beløpFørSamordning = 12_000.toBigDecimal())
        val vedtaksperiode = Periode(fradato = LocalDate.parse("2020-05-01"),
                                     tildato = LocalDate.parse("2020-12-31"))
        assertThat(finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder = listOf(beløpsperiode),
                                                            vedtaksperiode = vedtaksperiode).first())
                .isEqualTo(beløpsperiode.copy(periode = vedtaksperiode.copy(fradato = LocalDate.parse("2020-09-01"),
                                                                            tildato = LocalDate.parse("2020-12-31"))))
    }
    @Test
    internal fun `hvis beløpsperiode har ingen overlapp med vedtaksperiode skal tom liste returneres`() {
        val beløpsperiode =
                Beløpsperiode(periode = Periode(fradato = LocalDate.parse("2020-01-01"),
                                                tildato = LocalDate.parse("2020-04-30")),
                              beløp = 10_000.toBigDecimal(),
                              beløpFørSamordning = 12_000.toBigDecimal())
        val vedtaksperiode = Periode(fradato = LocalDate.parse("2020-05-01"),
                                     tildato = LocalDate.parse("2020-12-31"))
        assertThat(finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder = listOf(beløpsperiode),
                                                            vedtaksperiode = vedtaksperiode))
                .isEqualTo(emptyList<Beløpsperiode>())
    }
}