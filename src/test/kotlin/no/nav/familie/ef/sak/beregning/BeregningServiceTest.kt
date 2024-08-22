package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

internal class BeregningServiceTest {
    private val beregningService = BeregningService()

    @Test
    internal fun `skal beregne full ytelse når det ikke foreligger inntekt`() {
        val beregningsgrunnlagG2018 =
            Beregningsgrunnlag(
                samordningsfradrag = BigDecimal.ZERO,
                inntekt = BigDecimal.ZERO,
                avkortningPerMåned = BigDecimal.ZERO,
                fullOvergangsStønadPerMåned = BigDecimal(18_166),
                grunnbeløp = 96883.toBigDecimal(),
            )
        val beregningsgrunnlagG2019 =
            Beregningsgrunnlag(
                samordningsfradrag = BigDecimal.ZERO,
                inntekt = BigDecimal.ZERO,
                avkortningPerMåned = BigDecimal.ZERO,
                fullOvergangsStønadPerMåned = BigDecimal(18_723),
                grunnbeløp = 99858.toBigDecimal(),
            )
        val beregningsgrunnlagG2020 =
            Beregningsgrunnlag(
                samordningsfradrag = BigDecimal.ZERO,
                inntekt = BigDecimal.ZERO,
                avkortningPerMåned = BigDecimal.ZERO,
                fullOvergangsStønadPerMåned = BigDecimal(19_003),
                grunnbeløp = 101351.toBigDecimal(),
            )

        val beregningsgrunnlagG2021 =
            Beregningsgrunnlag(
                samordningsfradrag = BigDecimal.ZERO,
                inntekt = BigDecimal.ZERO,
                avkortningPerMåned = BigDecimal.ZERO,
                fullOvergangsStønadPerMåned = BigDecimal(19_950),
                grunnbeløp = 106399.toBigDecimal(),
            )
        val fullYtelse =
            beregningService.beregnYtelse(
                inntektsperioder =
                    listOf(
                        Inntektsperiode(
                            periode =
                                Månedsperiode(
                                    LocalDate.parse("2019-04-30"),
                                    LocalDate.parse("2022-04-30"),
                                ),
                            inntekt = BigDecimal(0),
                            samordningsfradrag = BigDecimal(0),
                        ),
                    ),
                vedtaksperioder =
                    listOf(
                        Månedsperiode(
                            LocalDate.parse("2019-04-30"),
                            LocalDate.parse("2022-04-30"),
                        ),
                    ),
            )

        assertThat(fullYtelse.size).isEqualTo(4)
        assertThat(fullYtelse[0]).isEqualTo(
            Beløpsperiode(
                Månedsperiode(
                    LocalDate.parse("2019-04-30"),
                    LocalDate.parse("2019-04-30"),
                ),
                beregningsgrunnlag = beregningsgrunnlagG2018,
                beløp = 18166.toBigDecimal(),
                beløpFørSamordning = 18166.toBigDecimal(),
            ),
        )
        assertThat(fullYtelse[1]).isEqualTo(
            Beløpsperiode(
                Månedsperiode(
                    LocalDate.parse("2019-05-01"),
                    LocalDate.parse("2020-04-30"),
                ),
                beregningsgrunnlag = beregningsgrunnlagG2019,
                beløp = 18723.toBigDecimal(),
                beløpFørSamordning = 18723.toBigDecimal(),
            ),
        )
        assertThat(fullYtelse[2]).isEqualTo(
            Beløpsperiode(
                Månedsperiode(
                    LocalDate.parse("2020-05-01"),
                    LocalDate.parse("2021-04-30"),
                ),
                beregningsgrunnlag = beregningsgrunnlagG2020,
                beløp = 19003.toBigDecimal(),
                beløpFørSamordning = 19003.toBigDecimal(),
            ),
        )
        assertThat(fullYtelse[3]).isEqualTo(
            Beløpsperiode(
                Månedsperiode(
                    LocalDate.parse("2021-05-01"),
                    LocalDate.parse("2022-04-30"),
                ),
                beregningsgrunnlag = beregningsgrunnlagG2021,
                beløp = 19950.toBigDecimal(),
                beløpFørSamordning = 19950.toBigDecimal(),
            ),
        )
    }

    @Test
    internal fun `skal beregne periodebeløp når det foreligger inntekt`() {
        val grunnbeløp = 99858.toBigDecimal()
        val inntekt = BigDecimal(240_000)
        val fullOvergangsstønad = grunnbeløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12))

        val avkortning =
            inntekt
                .subtract(grunnbeløp.multiply(BigDecimal(0.5)))
                .multiply(BigDecimal(0.45))
                .divide(BigDecimal(12))
                .setScale(0, RoundingMode.HALF_DOWN)
        val beløpTilUtbetalning = fullOvergangsstønad.subtract(avkortning).setScale(0, RoundingMode.HALF_UP)

        val beregningsgrunnlagG2019 =
            Beregningsgrunnlag(
                samordningsfradrag = BigDecimal(0),
                inntekt = inntekt,
                avkortningPerMåned = avkortning,
                grunnbeløp = grunnbeløp,
                fullOvergangsStønadPerMåned =
                    fullOvergangsstønad.setScale(
                        0,
                        RoundingMode.HALF_DOWN,
                    ),
            )
        val fullYtelse =
            beregningService.beregnYtelse(
                inntektsperioder =
                    listOf(
                        Inntektsperiode(
                            periode =
                                Månedsperiode(
                                    LocalDate.parse("2019-06-01"),
                                    LocalDate.parse("2020-04-30"),
                                ),
                            inntekt = inntekt,
                            samordningsfradrag = BigDecimal(0),
                        ),
                    ),
                vedtaksperioder =
                    listOf(
                        Månedsperiode(
                            LocalDate.parse("2019-06-01"),
                            LocalDate.parse("2020-04-30"),
                        ),
                    ),
            )

        assertThat(fullYtelse.size).isEqualTo(1)
        assertThat(fullYtelse[0]).isEqualTo(
            Beløpsperiode(
                Månedsperiode(
                    LocalDate.parse("2019-06-01"),
                    LocalDate.parse("2020-04-30"),
                ),
                beregningsgrunnlag = beregningsgrunnlagG2019,
                beløp = beløpTilUtbetalning,
                beløpFørSamordning = beløpTilUtbetalning,
            ),
        )
    }

    @Test
    internal fun `skal beregne periodebeløp når det foreligger inntekt i begynnelsen av vedtaksperiode`() {
        val grunnbeløp2019 = 99858.toBigDecimal()
        val grunnbeløp2018 = 96883.toBigDecimal()

        val inntekt = BigDecimal(240_000)
        val fullOvergangsstønad2018PerMåned =
            grunnbeløp2018.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP)
        val avkortningPerMåned =
            inntekt
                .subtract(grunnbeløp2018.multiply(BigDecimal(0.5)))
                .multiply(BigDecimal(0.45))
                .setScale(5, RoundingMode.HALF_DOWN)
                .divide(BigDecimal(12))
                .setScale(0, RoundingMode.HALF_DOWN)

        val beløpTilUtbetalningIFørstePerioden =
            fullOvergangsstønad2018PerMåned.subtract(avkortningPerMåned).setScale(0, RoundingMode.HALF_UP)

        val fullOvergangsstønad2019 =
            grunnbeløp2019.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP)

        val beregningsgrunnlagIFørstePerioden =
            Beregningsgrunnlag(
                samordningsfradrag = BigDecimal.ZERO,
                inntekt = inntekt,
                avkortningPerMåned = avkortningPerMåned,
                fullOvergangsStønadPerMåned = fullOvergangsstønad2018PerMåned,
                grunnbeløp = grunnbeløp2018,
            )

        val beregningsgrunnlagIAndrePerioden =
            Beregningsgrunnlag(
                samordningsfradrag = BigDecimal.ZERO,
                inntekt = BigDecimal.ZERO,
                avkortningPerMåned = BigDecimal.ZERO,
                fullOvergangsStønadPerMåned = fullOvergangsstønad2019,
                grunnbeløp = grunnbeløp2019,
            )

        val fullYtelse =
            beregningService.beregnYtelse(
                inntektsperioder =
                    listOf(
                        Inntektsperiode(
                            periode =
                                Månedsperiode(
                                    LocalDate.parse("2019-01-01"),
                                    LocalDate.parse("2019-02-28"),
                                ),
                            inntekt = inntekt,
                            samordningsfradrag = BigDecimal(0),
                        ),
                        Inntektsperiode(
                            periode =
                                Månedsperiode(
                                    LocalDate.parse("2019-03-01"),
                                    LocalDate.parse("2026-06-30"),
                                ),
                            inntekt = BigDecimal(0),
                            samordningsfradrag = BigDecimal(0),
                        ),
                    ),
                vedtaksperioder =
                    listOf(
                        Månedsperiode(
                            LocalDate.parse("2019-01-01"),
                            LocalDate.parse("2019-02-28"),
                        ),
                        Månedsperiode(
                            LocalDate.parse("2019-06-01"),
                            LocalDate.parse("2020-04-30"),
                        ),
                    ),
            )
        assertThat(fullYtelse.size).isEqualTo(2)
        assertThat(fullYtelse[0]).isEqualTo(
            Beløpsperiode(
                Månedsperiode(
                    LocalDate.parse("2019-01-01"),
                    LocalDate.parse("2019-02-28"),
                ),
                beregningsgrunnlag = beregningsgrunnlagIFørstePerioden,
                beløp = beløpTilUtbetalningIFørstePerioden,
                beløpFørSamordning = beløpTilUtbetalningIFørstePerioden,
            ),
        )

        assertThat(fullYtelse[1]).isEqualTo(
            Beløpsperiode(
                Månedsperiode(
                    LocalDate.parse("2019-06-01"),
                    LocalDate.parse("2020-04-30"),
                ),
                beregningsgrunnlag = beregningsgrunnlagIAndrePerioden,
                beløp = fullOvergangsstønad2019,
                beløpFørSamordning = fullOvergangsstønad2019,
            ),
        )
    }

    @Test
    internal fun `skal beregne periodebeløp til 0 når det foreligger inntekt større enn 5,5G `() {
        val grunnbeløp2017 = 101351.toBigDecimal()
        val inntekt = grunnbeløp2017.multiply(BigDecimal(5.51))

        val vedtakperioder =
            listOf(
                Månedsperiode(
                    LocalDate.parse("2020-05-01"),
                    LocalDate.parse("2023-04-30"),
                ),
            )

        val inntektsperioder =
            listOf(
                Inntektsperiode(
                    periode =
                        Månedsperiode(
                            LocalDate.parse("2019-01-01"),
                            LocalDate.parse("2024-04-30"),
                        ),
                    inntekt = inntekt,
                    samordningsfradrag = BigDecimal.ZERO,
                ),
            )

        val ytelseTilUtbetalning =
            beregningService.beregnYtelse(inntektsperioder = inntektsperioder, vedtaksperioder = vedtakperioder)
        assertThat(ytelseTilUtbetalning[0].beløp).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    internal fun `skal feile hvis inntektsperioder ikke dekker vedtaksperioder`() {
        val inntekt = BigDecimal(240_000)

        val vedtakperiode =
            Månedsperiode(
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-04-28"),
            )
        val inntektsperiode =
            Inntektsperiode(
                periode =
                    Månedsperiode(
                        LocalDate.parse("2019-01-01"),
                        LocalDate.parse("2019-02-28"),
                    ),
                inntekt = inntekt,
                samordningsfradrag = 0.toBigDecimal(),
            )

        assertThrows<ApiFeil> {
            beregningService.beregnYtelse(
                inntektsperioder = listOf(inntektsperiode),
                vedtaksperioder = listOf(vedtakperiode),
            )
        }
    }

    @Test
    internal fun `skal feil hvis inntektsperioder overlapper`() {
        val inntekt = BigDecimal(240_000)

        val vedtakperiode =
            Månedsperiode(
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-04-28"),
            )
        val inntektsperioder =
            listOf(
                Inntektsperiode(
                    periode =
                        Månedsperiode(
                            LocalDate.parse("2019-01-01"),
                            LocalDate.parse("2019-02-28"),
                        ),
                    inntekt = inntekt,
                    samordningsfradrag = 0.toBigDecimal(),
                ),
                Inntektsperiode(
                    periode =
                        Månedsperiode(
                            LocalDate.parse("2019-01-01"),
                            LocalDate.parse("2019-04-28"),
                        ),
                    inntekt = inntekt,
                    samordningsfradrag = 0.toBigDecimal(),
                ),
            )

        assertThrows<ApiFeil> {
            (
                beregningService.beregnYtelse(
                    inntektsperioder = inntektsperioder,
                    vedtaksperioder = listOf(vedtakperiode),
                )
            )
        }
    }

    @Test
    internal fun `skal feile hvis vedtaksperioder overlapper`() {
        val inntekt = BigDecimal(240_000)

        val vedtakperioder =
            listOf(
                Månedsperiode(
                    LocalDate.parse("2019-01-01"),
                    LocalDate.parse("2019-04-28"),
                ),
                Månedsperiode(
                    LocalDate.parse("2019-03-01"),
                    LocalDate.parse("2019-06-28"),
                ),
            )
        val inntektsperioder =
            listOf(
                Inntektsperiode(
                    periode =
                        Månedsperiode(
                            LocalDate.parse("2019-01-01"),
                            LocalDate.parse("2019-06-28"),
                        ),
                    inntekt = inntekt,
                    samordningsfradrag = 0.toBigDecimal(),
                ),
            )

        assertThrows<ApiFeil> {
            beregningService.beregnYtelse(
                inntektsperioder = inntektsperioder,
                vedtaksperioder = vedtakperioder,
            )
        }
    }

    @Test
    fun `grunnbeløpsperiodeDTO skal returnere korrekt seks ganger grunnbeløp per måned`() {
        val g = 124028.toBigDecimal()
        val grunnbeløp =
            Grunnbeløp(
                periode = Månedsperiode(LocalDate.of(2024, 5, 1), LocalDate.of(2025, 4, 30)),
                grunnbeløp = g,
                perMnd = 10335.toBigDecimal(), // G 124028/12 = 10335.6666667
            )
        val grunnbeløpsperiodeDTO = beregningService.grunnbeløpsperiodeDTO(grunnbeløp)

        val korrektSeksGangerGrunnbeløpPerMåned = 62014.toBigDecimal()

        Assertions.assertEquals(korrektSeksGangerGrunnbeløpPerMåned, grunnbeløpsperiodeDTO.seksGangerGrunnbeløpPerMåned)
    }
}
