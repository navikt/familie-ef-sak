package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.util.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

internal class BeregningServiceTest {

    private val beregningService = BeregningService()


    @Test
    internal fun `skal beregne full ytelse når det ikke foreligger inntekt`() {
        val beregningsgrunnlagG2018 =
                Beregningsgrunnlag(samordningsfradrag = BigDecimal.ZERO,
                                   inntekt = BigDecimal.ZERO,
                                   avkortningPerMåned = BigDecimal.ZERO,
                                   fullOvergangsStønadPerMåned = BigDecimal(18_166),
                                   grunnbeløp = 96883.toBigDecimal())
        val beregningsgrunnlagG2019 =
                Beregningsgrunnlag(samordningsfradrag = BigDecimal.ZERO,
                                   inntekt = BigDecimal.ZERO,
                                   avkortningPerMåned = BigDecimal.ZERO,
                                   fullOvergangsStønadPerMåned = BigDecimal(18_723),
                                   grunnbeløp = 99858.toBigDecimal())
        val beregningsgrunnlagG2020 = Beregningsgrunnlag(samordningsfradrag = BigDecimal.ZERO,
                                                         inntekt = BigDecimal.ZERO,
                                                         avkortningPerMåned = BigDecimal.ZERO,
                                                         fullOvergangsStønadPerMåned = BigDecimal(19_003),
                                                         grunnbeløp = 101351.toBigDecimal())
        val fullYtelse = beregningService.beregnYtelse(inntektsperioder = listOf(Inntektsperiode(LocalDate.parse("2019-04-30"),
                                                                                                 LocalDate.parse("2022-04-30"),
                                                                                                 BigDecimal(0),
                                                                                                 BigDecimal(0))),
                                                       vedtaksperioder = listOf(Periode(LocalDate.parse("2019-04-30"),
                                                                                        LocalDate.parse("2022-04-30"))))

        assertThat(fullYtelse.size).isEqualTo(3)
        assertThat(fullYtelse[0]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-04-30"),
                                                          LocalDate.parse("2019-05-01"),
                                                          beregningsgrunnlagG2018,
                                                          18166.toBigDecimal()))
        assertThat(fullYtelse[1]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-05-01"),
                                                          LocalDate.parse("2020-05-01"),
                                                          beregningsgrunnlagG2019,
                                                          18723.toBigDecimal()))
        assertThat(fullYtelse[2]).isEqualTo(Beløpsperiode(LocalDate.parse("2020-05-01"),
                                                          LocalDate.parse("2022-04-30"),
                                                          beregningsgrunnlagG2020,
                                                          19003.toBigDecimal()))
    }

    @Test
    internal fun `skal beregne periodebeløp når det foreligger inntekt`() {
        val grunnbeløp = 99858.toBigDecimal()
        val inntekt = BigDecimal(240_000)
        val fullOvergangsstønad = grunnbeløp.multiply(BigDecimal(2.25)).divide(BigDecimal(12))

        val avkortning = inntekt.subtract(grunnbeløp.multiply(BigDecimal(0.5)))
                .multiply(BigDecimal(0.45))
                .divide(BigDecimal(12))
                .setScale(0, RoundingMode.HALF_DOWN)
        val beløpTilUtbetalning = fullOvergangsstønad.subtract(avkortning).setScale(0, RoundingMode.HALF_UP)


        val beregningsgrunnlagG2019 = Beregningsgrunnlag(samordningsfradrag = BigDecimal(0),
                                                         inntekt = inntekt,
                                                         avkortningPerMåned = avkortning,
                                                         grunnbeløp = grunnbeløp,
                                                         fullOvergangsStønadPerMåned = fullOvergangsstønad.setScale(0,
                                                                                                                    RoundingMode.HALF_DOWN))
        val fullYtelse = beregningService.beregnYtelse(inntektsperioder =
                                                       listOf(Inntektsperiode(startDato = LocalDate.parse("2019-06-01"),
                                                                              sluttDato = LocalDate.parse("2020-04-30"),
                                                                              inntekt = inntekt,
                                                                              samordningsfradrag = BigDecimal(0))),
                                                       vedtaksperioder = listOf(Periode(LocalDate.parse("2019-06-01"),
                                                                                        LocalDate.parse("2020-04-30")))
        )



        assertThat(fullYtelse.size).isEqualTo(1)
        assertThat(fullYtelse[0]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-06-01"),
                                                          LocalDate.parse("2020-04-30"),
                                                          beregningsgrunnlagG2019,
                                                          beløpTilUtbetalning))

    }

    @Test
    internal fun `skal beregne periodebeløp når det foreligger inntekt i begynnelsen av vedtaksperiode`() {
        val grunnbeløp2019 = 99858.toBigDecimal()
        val grunnbeløp2018 = 96883.toBigDecimal()

        val inntekt = BigDecimal(240_000)
        val fullOvergangsstønad2018PerMåned =
                grunnbeløp2018.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP)
        val avkortningPerMåned = inntekt.subtract(grunnbeløp2018.multiply(BigDecimal(0.5)))
                .multiply(BigDecimal(0.45))
                .setScale(5, RoundingMode.HALF_DOWN)
                .divide(BigDecimal(12))
                .setScale(0, RoundingMode.HALF_DOWN)


        val beløpTilUtbetalningIFørstePerioden =
                fullOvergangsstønad2018PerMåned.subtract(avkortningPerMåned).setScale(0, RoundingMode.HALF_UP)


        val fullOvergangsstønad2019 =
                grunnbeløp2019.multiply(BigDecimal(2.25)).divide(BigDecimal(12)).setScale(0, RoundingMode.HALF_UP)
        val beløpTilUtbetalningIAndraPerioden = fullOvergangsstønad2019


        val beregningsgrunnlagIFørstePerioden = Beregningsgrunnlag(samordningsfradrag = BigDecimal.ZERO,
                                                                   inntekt = inntekt,
                                                                   avkortningPerMåned = avkortningPerMåned,
                                                                   fullOvergangsStønadPerMåned = fullOvergangsstønad2018PerMåned,
                                                                   grunnbeløp = grunnbeløp2018)

        val beregningsgrunnlagIAndrePerioden = Beregningsgrunnlag(samordningsfradrag = BigDecimal.ZERO,
                                                                  inntekt = BigDecimal.ZERO,
                                                                  avkortningPerMåned = BigDecimal.ZERO,
                                                                  fullOvergangsStønadPerMåned = fullOvergangsstønad2019,
                                                                  grunnbeløp = grunnbeløp2019)


        val fullYtelse = beregningService.beregnYtelse(
                inntektsperioder = listOf(Inntektsperiode(startDato = LocalDate.parse("2019-01-01"),
                                                          sluttDato = LocalDate.parse("2019-02-28"),
                                                          inntekt = inntekt,
                                                          samordningsfradrag = BigDecimal(0)),
                                          Inntektsperiode(startDato = LocalDate.parse("2019-03-01"),
                                                          sluttDato = LocalDate.parse("2026-06-30"),
                                                          inntekt = BigDecimal(0),
                                                          samordningsfradrag = BigDecimal(0))
                ),
                vedtaksperioder = listOf(Periode(LocalDate.parse("2019-01-01"),
                                                 LocalDate.parse("2019-02-28")),
                                         Periode(LocalDate.parse("2019-06-01"),
                                                 LocalDate.parse("2020-04-30")))
        )
        assertThat(fullYtelse.size).isEqualTo(2)
        assertThat(fullYtelse[0]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-01-01"),
                                                          LocalDate.parse("2019-02-28"),
                                                          beregningsgrunnlagIFørstePerioden,
                                                          beløpTilUtbetalningIFørstePerioden))

        assertThat(fullYtelse[1]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-06-01"),
                                                          LocalDate.parse("2020-04-30"),
                                                          beregningsgrunnlagIAndrePerioden,
                                                          beløpTilUtbetalningIAndraPerioden))

    }


    @Test
    internal fun `skal beregne periodebeløp til 0 når det foreligger inntekt større enn 5,5G `() {
        val grunnbeløp2017 = 101351.toBigDecimal()
        val inntekt = grunnbeløp2017.multiply(BigDecimal(5.51))

        val vedtakperioder = listOf(Periode(LocalDate.parse("2020-05-01"),
                                            LocalDate.parse("2023-04-30")))

        val inntektsperioder = listOf(Inntektsperiode(startDato = LocalDate.parse("2019-01-01"),
                                                      sluttDato = LocalDate.parse("2024-04-30"),
                                                      inntekt = inntekt,
                                                      samordningsfradrag = BigDecimal.ZERO))


        val ytelseTilUtbetalning = beregningService.beregnYtelse(inntektsperioder = inntektsperioder, vedtaksperioder = vedtakperioder)
        assertThat(ytelseTilUtbetalning[0].beløp).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    internal fun `skal feile hvis inntektsperioder ikke dekker vedtaksperioder`() {
        val inntekt = BigDecimal(240_000)

        val vedtakperiode = Periode(LocalDate.parse("2019-01-01"),
                                    LocalDate.parse("2019-04-28"))
        val inntektsperiode = Inntektsperiode(startDato = LocalDate.parse("2019-01-01"),
                                              sluttDato = LocalDate.parse("2019-02-28"),
                                              inntekt = inntekt,
                                              samordningsfradrag = 0.toBigDecimal())

        assertThrows<Feil> {
            beregningService.beregnYtelse(inntektsperioder = listOf(inntektsperiode),
                                          vedtaksperioder = listOf(vedtakperiode))
        }
    }

    @Test
    internal fun `skal feil hvis inntektsperioder overlapper`() {
        val inntekt = BigDecimal(240_000)

        val vedtakperiode = Periode(LocalDate.parse("2019-01-01"),
                                    LocalDate.parse("2019-04-28"))
        val inntektsperioder = listOf(Inntektsperiode(startDato = LocalDate.parse("2019-01-01"),
                                                      sluttDato = LocalDate.parse("2019-02-28"),
                                                      inntekt = inntekt,
                                                      samordningsfradrag = 0.toBigDecimal()),
                                      Inntektsperiode(startDato = LocalDate.parse("2019-01-01"),
                                                      sluttDato = LocalDate.parse("2019-04-28"),
                                                      inntekt = inntekt,
                                                      samordningsfradrag = 0.toBigDecimal()))


        assertThrows<Feil> {
            (beregningService.beregnYtelse(inntektsperioder = inntektsperioder,
                                           vedtaksperioder = listOf(vedtakperiode)))
        }
    }

    @Test
    internal fun `skal feile hvis vedtaksperioder overlapper`() {
        val inntekt = BigDecimal(240_000)

        val vedtakperioder = listOf(Periode(LocalDate.parse("2019-01-01"),
                                            LocalDate.parse("2019-04-28")),
                                    Periode(LocalDate.parse("2019-03-01"),
                                            LocalDate.parse("2019-06-28")))
        val inntektsperioder = listOf(Inntektsperiode(startDato = LocalDate.parse("2019-01-01"),
                                                      sluttDato = LocalDate.parse("2019-06-28"),
                                                      inntekt = inntekt,
                                                      samordningsfradrag = 0.toBigDecimal()))

        assertThrows<Feil> {
            beregningService.beregnYtelse(inntektsperioder = inntektsperioder,
                                          vedtaksperioder = vedtakperioder)
        }
    }

}