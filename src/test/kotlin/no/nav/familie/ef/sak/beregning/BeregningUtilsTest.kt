package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.BeregningUtils.finnStartDatoOgSluttDatoForBeløpsperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

internal class BeregningUtilsTest {
    @Nested
    inner class FinnStartDatoOgSluttDatoForBeløpsperiode {
        @Test
        fun `hvis vedtaksperiode omsluttes av beløpsperiode skal datoerne for vedtaksperiode returneres `() {
            val beløpsperiode =
                Beløpsperiode(
                    periode =
                        Månedsperiode(
                            fom = LocalDate.parse("2020-05-01"),
                            tom = LocalDate.parse("2020-12-01"),
                        ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal(),
                )
            val vedtaksperiode = Månedsperiode(fom = LocalDate.parse("2020-07-01"), tom = LocalDate.parse("2020-10-31"))
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode,
                ).first(),
            ).isEqualTo(beløpsperiode.copy(periode = vedtaksperiode))
        }

        @Test
        fun `hvis beløpsperiode omsluttes av vedtaksperiode skal datoerne for beløpsperiode være uforandrede`() {
            val beløpsperiode =
                Beløpsperiode(
                    periode =
                        Månedsperiode(
                            fom = LocalDate.parse("2020-07-01"),
                            tom = LocalDate.parse("2020-09-30"),
                        ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal(),
                )
            val vedtaksperiode = Månedsperiode(fom = LocalDate.parse("2020-05-01"), tom = LocalDate.parse("2020-12-31"))
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode,
                ).first(),
            ).isEqualTo(beløpsperiode)
        }

        @Test
        fun `hvis beløpsperiode overlapper i starten av vedtaksperiode skal startdatoen for vedtaksperiode returneres sammen med sluttdato for beløpsperiode`() {
            val beløpsperiode =
                Beløpsperiode(
                    periode =
                        Månedsperiode(
                            fom = LocalDate.parse("2020-03-01"),
                            tom = LocalDate.parse("2020-06-30"),
                        ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal(),
                )
            val vedtaksperiode =
                Månedsperiode(
                    fom = LocalDate.parse("2020-05-01"),
                    tom = LocalDate.parse("2020-12-31"),
                )
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode,
                ).first(),
            ).isEqualTo(
                beløpsperiode.copy(
                    periode =
                        vedtaksperiode.copy(
                            fom = YearMonth.parse("2020-05"),
                            tom = YearMonth.parse("2020-06"),
                        ),
                ),
            )
        }

        @Test
        fun `hvis beløpsperiode overlapper i slutten av vedtaksperiode skal startdatoen for beløpsperiode returneres sammen med sluttdato for vedtaksperiode`() {
            val beløpsperiode =
                Beløpsperiode(
                    periode =
                        Månedsperiode(
                            fom = LocalDate.parse("2020-09-01"),
                            tom = LocalDate.parse("2021-02-28"),
                        ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal(),
                )
            val vedtaksperiode =
                Månedsperiode(
                    fom = LocalDate.parse("2020-05-01"),
                    tom = LocalDate.parse("2020-12-31"),
                )
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode,
                ).first(),
            ).isEqualTo(
                beløpsperiode.copy(
                    periode =
                        vedtaksperiode.copy(
                            fom = YearMonth.parse("2020-09"),
                            tom = YearMonth.parse("2020-12"),
                        ),
                ),
            )
        }

        @Test
        fun `hvis beløpsperiode har ingen overlapp med vedtaksperiode skal tom liste returneres`() {
            val beløpsperiode =
                Beløpsperiode(
                    periode =
                        Månedsperiode(
                            fom = LocalDate.parse("2020-01-01"),
                            tom = LocalDate.parse("2020-04-30"),
                        ),
                    beløp = 10_000.toBigDecimal(),
                    beløpFørSamordning = 12_000.toBigDecimal(),
                )
            val vedtaksperiode =
                Månedsperiode(
                    fom = LocalDate.parse("2020-05-01"),
                    tom = LocalDate.parse("2020-12-31"),
                )
            assertThat(
                finnStartDatoOgSluttDatoForBeløpsperiode(
                    beløpForInnteksperioder = listOf(beløpsperiode),
                    vedtaksperiode = vedtaksperiode,
                ),
            ).isEqualTo(emptyList<Beløpsperiode>())
        }
    }

    @Nested
    inner class IndeksjusterInntekt {
        @Test
        fun `skal ikke endre periode før siste brukte grunnbeløpsdato`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 1, 1),
                                LocalDate.of(2021, 4, 30),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 5, 1),
                                LocalDate.of(2021, 12, 31),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                )

            val indeksjusterInntekt =
                BeregningUtils.indeksjusterInntekt(
                    YearMonth.of(2022, 5),
                    inntektsperioder,
                )

            assertThat(indeksjusterInntekt).hasSameElementsAs(inntektsperioder)
        }

        @Test
        fun `skal returnere listen urørt hvis siste grunnbeløpsdato er fom for nyeste grunnbeløp`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 1, 1),
                                LocalDate.of(2021, 4, 30),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 5, 1),
                                LocalDate.of(2021, 12, 31),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                )

            val indeksjusterInntekt =
                BeregningUtils.indeksjusterInntekt(
                    Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fom,
                    inntektsperioder,
                )

            assertThat(indeksjusterInntekt).isSameAs(inntektsperioder)
        }

        @Test
        fun `skal justere inntekt for perioder som har fått nytt grunnbeløp`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 1, 1),
                                LocalDate.of(2021, 4, 30),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 5, 1),
                                LocalDate.of(2021, 12, 31),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                )

            val indeksjusterInntekt =
                BeregningUtils.indeksjusterInntekt(
                    YearMonth.of(2020, 5),
                    inntektsperioder,
                )

            assertThat(indeksjusterInntekt.first()).isEqualTo(inntektsperioder.first())
            assertThat(indeksjusterInntekt[1].periode).isEqualTo(inntektsperioder[1].periode)
            assertThat(indeksjusterInntekt[1].inntekt).isEqualTo(209_900.toBigDecimal()) // runder ikke av inntektsgrunnlag
            assertThat(indeksjusterInntekt[1].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
        }

        @Test
        fun `skal justere inntekt og splitte perioder når nytt grunnbeløp etter fom på siste periode`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 4, 30),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2020, 5, 1),
                                LocalDate.of(2021, 12, 31),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                )

            val indeksjusterInntekt =
                BeregningUtils.indeksjusterInntekt(
                    YearMonth.of(2020, 5),
                    inntektsperioder,
                )

            assertThat(indeksjusterInntekt.first()).isEqualTo(inntektsperioder.first())
            assertThat(indeksjusterInntekt[1].periode.fom).isEqualTo(inntektsperioder[1].periode.fom)
            assertThat(indeksjusterInntekt[1].periode.tomDato).isEqualTo(LocalDate.of(2021, 4, 30))
            assertThat(indeksjusterInntekt[1].inntekt).isEqualTo(200_000.toBigDecimal())
            assertThat(indeksjusterInntekt[1].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
            assertThat(indeksjusterInntekt[2].periode.fomDato).isEqualTo(LocalDate.of(2021, 5, 1))
            assertThat(indeksjusterInntekt[2].periode.tom).isEqualTo(inntektsperioder[1].periode.tom)
            assertThat(indeksjusterInntekt[2].inntekt).isEqualTo(209_900.toBigDecimal())
            assertThat(indeksjusterInntekt[2].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
        }

        @Test
        fun `skal justere inntekt også ved flere endringer i grunnbeløp`() {
            val inntektsperioder: List<Inntektsperiode> =
                listOf(
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 1, 1),
                                LocalDate.of(2021, 4, 30),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                    Inntektsperiode(
                        periode =
                            Månedsperiode(
                                LocalDate.of(2021, 5, 1),
                                LocalDate.of(2021, 12, 31),
                            ),
                        inntekt = 200_000.toBigDecimal(),
                        samordningsfradrag = BigDecimal(10),
                    ),
                )

            val indeksjusterInntekt =
                BeregningUtils.indeksjusterInntekt(
                    YearMonth.of(2019, 5),
                    inntektsperioder,
                )

            assertThat(indeksjusterInntekt.first().periode).isEqualTo(inntektsperioder.first().periode)
            assertThat(indeksjusterInntekt.first().inntekt).isEqualTo(202_900.toBigDecimal())
            assertThat(indeksjusterInntekt.first().samordningsfradrag).isEqualTo(inntektsperioder.first().samordningsfradrag)
            assertThat(indeksjusterInntekt[1].periode).isEqualTo(inntektsperioder[1].periode)
            assertThat(indeksjusterInntekt[1].inntekt).isEqualTo(213_100.toBigDecimal())
            assertThat(indeksjusterInntekt[1].samordningsfradrag).isEqualTo(inntektsperioder[1].samordningsfradrag)
        }
    }

    @Nested
    inner class BeregningAvTotalinntekt {
        val inntektsperiode =
            Inntektsperiode(
                periode =
                    Månedsperiode(
                        LocalDate.of(2021, 1, 1),
                        LocalDate.of(2021, 4, 30),
                    ),
                inntekt = BigDecimal.ZERO,
                samordningsfradrag = BigDecimal.ZERO,
            )

        @Test
        internal fun `skal bruke totalinntekten for å beregne beløp`() {
            val dagsats = 500.toBigDecimal()
            val månedsinntekt = 10_000.toBigDecimal()
            val årsinntekt = 100_000.toBigDecimal()
            val inntektsperiodeMedÅrsinntekt =
                inntektsperiode.copy(dagsats = dagsats, månedsinntekt = månedsinntekt, inntekt = årsinntekt)

            val resultat = BeregningUtils.beregnStønadForInntekt(inntektsperiodeMedÅrsinntekt).single()
            val forventetTotalinntekt =
                dagsats.multiply(BigDecimal(260)) + månedsinntekt.multiply(BigDecimal(12)) + årsinntekt
            assertThat(resultat.beregningsgrunnlag?.inntekt).isEqualTo((forventetTotalinntekt))
            assertThat(resultat.beløp).isEqualTo(7778.toBigDecimal())
        }

        @Test
        internal fun `skal runde beregnet inntekt ned til nærmeste 1000`() {
            val årsinntekt = 500_501.toBigDecimal()
            val inntektsperiodeMedÅrsinntekt = inntektsperiode.copy(inntekt = årsinntekt)

            val resultat = BeregningUtils.beregnStønadForInntekt(inntektsperiodeMedÅrsinntekt).single()
            assertThat(resultat.beregningsgrunnlag?.inntekt).isEqualTo((500_000.toBigDecimal()))
        }

        @Test
        internal fun `skal runde beregnet inntekt ned til nærmeste 1000 ved både års og månedsinntekt`() {
            val årsinntekt = 500_500.toBigDecimal()
            val månedsinntekt = 1000.toBigDecimal()
            val inntektsperiodeMedÅrsinntekt = inntektsperiode.copy(inntekt = årsinntekt, månedsinntekt = månedsinntekt)

            val resultat = BeregningUtils.beregnStønadForInntekt(inntektsperiodeMedÅrsinntekt).single()
            assertThat(resultat.beregningsgrunnlag?.inntekt).isEqualTo((512_000.toBigDecimal()))
        }

        @Test
        internal fun `skal runde beregnet inntekt ned til nærmeste 1000 ved både årsinntekt og dagsats`() {
            val årsinntekt = 500_500.toBigDecimal()
            val dagsats = 100.toBigDecimal()
            val inntektsperiodeMedÅrsinntekt = inntektsperiode.copy(inntekt = årsinntekt, dagsats = dagsats)

            val resultat = BeregningUtils.beregnStønadForInntekt(inntektsperiodeMedÅrsinntekt).single()
            assertThat(resultat.beregningsgrunnlag?.inntekt).isEqualTo((526_000.toBigDecimal()))
        }

        @Test
        internal fun `skal ikke runde beregnet inntekt ned til nærmeste 1000 hvis kun årsinntekt finnes og denne allerede er rundet til nærmeste 100`() {
            val årsinntekt = 500_500.toBigDecimal()
            val inntektsperiodeMedÅrsinntekt = inntektsperiode.copy(inntekt = årsinntekt)

            val resultat = BeregningUtils.beregnStønadForInntekt(inntektsperiodeMedÅrsinntekt).single()
            assertThat(resultat.beregningsgrunnlag?.inntekt).isEqualTo((500_500.toBigDecimal()))
        }

        @Test
        internal fun `Skal beregne måneds inntekt fra årsinntekt`() {
            val årsinntekt = 437_000.toBigDecimal()
            val inntektsperiodeMedÅrsinntekt = inntektsperiode.copy(inntekt = årsinntekt)

            val resultat = BeregningUtils.beregn10ProsentOppOgNedIMånedsinntektFraÅrsinntekt(inntektsperiodeMedÅrsinntekt)

            assertThat(resultat.opp).isEqualTo(40058)
            assertThat(resultat.ned).isEqualTo(32775)
        }

        @Test
        internal fun `skal beregne totalinntekt med månedsinntekt 21468 og årsinntekt 60000`() {
            val månedsinntekt = 21_468.toBigDecimal()
            val årsinntekt = 60_000.toBigDecimal()
            val inntektsperiodeMedMånedOgÅrsinntekt = inntektsperiode.copy(månedsinntekt = månedsinntekt, inntekt = årsinntekt)

            val resultat = BeregningUtils.beregnTotalinntekt(inntektsperiodeMedMånedOgÅrsinntekt)

            assertThat(resultat).isEqualTo(317_000.toBigDecimal())
        }
    }
}
