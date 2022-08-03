package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.BeregningUtils.finnStartDatoOgSluttDatoForBeløpsperiode
import no.nav.familie.kontrakter.felles.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class BeregningUtilsTest {

    @Nested
    inner class FinnStartDatoOgSluttDatoForBeløpsperiode {

        @Test
        fun `hvis vedtaksperiode omsluttes av beløpsperiode skal datoerne for vedtaksperiode returneres `() {
            val beløpsperiode = Beløpsperiode(
                fellesperiode = Periode(
                    fomDato = LocalDate.parse("2020-05-01"),
                    tomDato = LocalDate.parse("2020-12-01")
                ),
                beløp = 10_000.toBigDecimal(),
                beløpFørSamordning = 12_000.toBigDecimal()
            )
            val vedtaksperiode = Periode(fomDato = LocalDate.parse("2020-07-01"), tomDato = LocalDate.parse("2020-10-31"))
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode
                ).first()
            )
                .isEqualTo(beløpsperiode.copy(fellesperiode = vedtaksperiode, periode = no.nav.familie.ef.sak.felles.dto.Periode(vedtaksperiode.fomDato, vedtaksperiode.tomDato)))
        }

        @Test
        fun `hvis beløpsperiode omsluttes av vedtaksperiode skal datoerne for beløpsperiode være uforandrede`() {
            val beløpsperiode =
                Beløpsperiode(
                    fellesperiode = Periode(
                        fomDato = LocalDate.parse("2020-07-01"),
                        tomDato = LocalDate.parse("2020-09-30")
                    ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal()
                )
            val vedtaksperiode = Periode(fomDato = LocalDate.parse("2020-05-01"), tomDato = LocalDate.parse("2020-12-31"))
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode
                ).first()
            )
                .isEqualTo(beløpsperiode)
        }

        @Test
        fun `hvis beløpsperiode overlapper i starten av vedtaksperiode skal startdatoen for vedtaksperiode returneres sammen med sluttdato for beløpsperiode`() {
            val beløpsperiode =
                Beløpsperiode(
                    fellesperiode = Periode(
                        fomDato = LocalDate.parse("2020-03-01"),
                        tomDato = LocalDate.parse("2020-06-30")
                    ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal()
                )
            val vedtaksperiode = Periode(
                fomDato = LocalDate.parse("2020-05-01"),
                tomDato = LocalDate.parse("2020-12-31")
            )
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode
                ).first()
            )
                .isEqualTo(
                    beløpsperiode.copy(
                        fellesperiode = vedtaksperiode.copy(
                            fomDato = LocalDate.parse("2020-05-01"),
                            tomDato = LocalDate.parse("2020-06-30")
                        ),
                        periode = no.nav.familie.ef.sak.felles.dto.Periode(
                            fradato = LocalDate.parse("2020-05-01"),
                            tildato = LocalDate.parse("2020-06-30")
                        )
                    )
                )
        }

        @Test
        fun `hvis beløpsperiode overlapper i slutten av vedtaksperiode skal startdatoen for beløpsperiode returneres sammen med sluttdato for vedtaksperiode`() {
            val beløpsperiode =
                Beløpsperiode(
                    fellesperiode = Periode(
                        fomDato = LocalDate.parse("2020-09-01"),
                        tomDato = LocalDate.parse("2021-02-28")
                    ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal()
                )
            val vedtaksperiode = Periode(
                fomDato = LocalDate.parse("2020-05-01"),
                tomDato = LocalDate.parse("2020-12-31")
            )
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode
                ).first()
            )
                .isEqualTo(
                    beløpsperiode.copy(
                        periode = no.nav.familie.ef.sak.felles.dto.Periode(
                            fradato = LocalDate.parse("2020-09-01"),
                            tildato = LocalDate.parse("2020-12-31")
                        ),
                        fellesperiode = vedtaksperiode.copy(
                            fomDato = LocalDate.parse("2020-09-01"),
                            tomDato = LocalDate.parse("2020-12-31")
                        )
                    )
                )
        }

        @Test
        fun `hvis beløpsperiode har ingen overlapp med vedtaksperiode skal tom liste returneres`() {
            val beløpsperiode =
                Beløpsperiode(
                    fellesperiode = Periode(
                        fomDato = LocalDate.parse("2020-01-01"),
                        tomDato = LocalDate.parse("2020-04-30")
                    ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal()
                )
            val vedtaksperiode = Periode(
                fomDato = LocalDate.parse("2020-05-01"),
                tomDato = LocalDate.parse("2020-12-31")
            )
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode
                )
            )
                .isEqualTo(emptyList<Beløpsperiode>())
        }
    }

    @Nested
    inner class IndeksjusterInntekt {

        @Test
        fun `skal ikke endre periode før siste brukte grunnbeløpsdato`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 1, 1),
                            LocalDate.of(2021, 4, 30)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    ),
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 5, 1),
                            LocalDate.of(2021, 12, 31)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    )
                )

            val indeksjusterInntekt = BeregningUtils.indeksjusterInntekt(
                LocalDate.of(2022, 5, 1),
                inntektsperioder
            )

            assertThat(indeksjusterInntekt).hasSameElementsAs(inntektsperioder)
        }

        @Test
        fun `skal returnere listen urørt hvis siste grunnbeløpsdato er fomDato for nyeste grunnbeløp`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 1, 1),
                            LocalDate.of(2021, 4, 30)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    ),
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 5, 1),
                            LocalDate.of(2021, 12, 31)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    )
                )

            val indeksjusterInntekt = BeregningUtils.indeksjusterInntekt(
                nyesteGrunnbeløp.periode.fomDato,
                inntektsperioder
            )

            assertThat(indeksjusterInntekt).isSameAs(inntektsperioder)
        }

        @Test
        fun `skal justere inntekt for perioder som har fått nytt grunnbeløp`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 1, 1),
                            LocalDate.of(2021, 4, 30)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    ),
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 5, 1),
                            LocalDate.of(2021, 12, 31)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    )
                )

            val indeksjusterInntekt = BeregningUtils.indeksjusterInntekt(
                LocalDate.of(2020, 5, 1),
                inntektsperioder
            )

            assertThat(indeksjusterInntekt.first()).isEqualTo(inntektsperioder.first())
            assertThat(indeksjusterInntekt[1].periode).isEqualTo(inntektsperioder[1].periode)
            assertThat(indeksjusterInntekt[1].inntekt).isEqualTo(209_900.toBigDecimal())
            assertThat(indeksjusterInntekt[1].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
        }

        @Test
        fun `skal justere inntekt og splitte perioder når nytt grunnbeløp etter fomDato på siste periode`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2020, 1, 1),
                            LocalDate.of(2020, 4, 30)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    ),
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2020, 5, 1),
                            LocalDate.of(2021, 12, 31)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    )
                )

            val indeksjusterInntekt = BeregningUtils.indeksjusterInntekt(
                LocalDate.of(2020, 5, 1),
                inntektsperioder
            )

            assertThat(indeksjusterInntekt.first()).isEqualTo(inntektsperioder.first())
            assertThat(indeksjusterInntekt[1].periode.fomDato).isEqualTo(inntektsperioder[1].periode.fomDato)
            assertThat(indeksjusterInntekt[1].periode.tomDato).isEqualTo(LocalDate.of(2021, 4, 30))
            assertThat(indeksjusterInntekt[1].inntekt).isEqualTo(200_000.toBigDecimal())
            assertThat(indeksjusterInntekt[1].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
            assertThat(indeksjusterInntekt[2].periode.fomDato).isEqualTo(LocalDate.of(2021, 5, 1))
            assertThat(indeksjusterInntekt[2].periode.tomDato).isEqualTo(inntektsperioder[1].periode.tomDato)
            assertThat(indeksjusterInntekt[2].inntekt).isEqualTo(209_900.toBigDecimal())
            assertThat(indeksjusterInntekt[2].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
        }

        @Test
        fun `skal justere inntekt også ved flere endringer i grunnbeløp`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 1, 1),
                            LocalDate.of(2021, 4, 30)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    ),
                    Inntektsperiode(
                        Periode(
                            LocalDate.of(2021, 5, 1),
                            LocalDate.of(2021, 12, 31)
                        ),
                        200_000.toBigDecimal(),
                        BigDecimal(10)
                    )
                )

            val indeksjusterInntekt = BeregningUtils.indeksjusterInntekt(
                LocalDate.of(2019, 5, 1),
                inntektsperioder
            )

            assertThat(indeksjusterInntekt.first().periode).isEqualTo(inntektsperioder.first().periode)
            assertThat(indeksjusterInntekt.first().inntekt).isEqualTo(202_900.toBigDecimal())
            assertThat(indeksjusterInntekt.first().samordningsfradrag).isEqualTo(inntektsperioder.first().samordningsfradrag)
            assertThat(indeksjusterInntekt[1].periode).isEqualTo(inntektsperioder[1].periode)
            assertThat(indeksjusterInntekt[1].inntekt).isEqualTo(213_100.toBigDecimal())
            assertThat(indeksjusterInntekt[1].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
        }
    }
}
