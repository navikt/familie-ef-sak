package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.util.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class BeregningServiceTest {

    private val beregningService = BeregningService()

    @Test
    internal fun `skal beregne full ytelse når det ikke foreligger inntekt`() {
        val beregningsgrunnlag = Beregningsgrunnlag(samordningsfradrag = BigDecimal(0), inntekt = BigDecimal(0), grunnbeløp = 101351.toBigDecimal())
        val fullYtelse = beregningService.beregnYtelse(BeregningRequest(
                listOf(Inntektsperiode(LocalDate.parse("2019-04-30"),
                                       LocalDate.parse("2022-04-30"),
                                       BigDecimal(0), BigDecimal(0))), listOf(Periode(LocalDate.parse("2019-04-30"),
                                                                                      LocalDate.parse("2022-04-30")))
        ))

        assertThat(fullYtelse.size).isEqualTo(3)
        assertThat(fullYtelse[0]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-04-30"),
                                                          LocalDate.parse("2019-05-01"),
                                                          beregningsgrunnlag.copy(grunnbeløp = 96883.toBigDecimal()),
                                                          18166.toBigDecimal()))
        assertThat(fullYtelse[1]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-05-01"),
                                                          LocalDate.parse("2020-05-01"),
                                                          beregningsgrunnlag.copy(grunnbeløp = 99858.toBigDecimal()),
                                                          18723.toBigDecimal()))
        assertThat(fullYtelse[2]).isEqualTo(Beløpsperiode(LocalDate.parse("2020-05-01"),
                                                          LocalDate.parse("2022-04-30"),
                                                          beregningsgrunnlag,
                                                          19003.toBigDecimal()))
    }

}