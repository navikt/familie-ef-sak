package no.nav.familie.ef.sak.api.beregning

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BeregningServiceTest {

    private val beregningService = BeregningService()

    @Test
    internal fun `skal beregne full ytelse når det ikke foreligger inntekt`() {
        val fullYtelse = beregningService.beregnFullYtelse(BeregningRequest(emptyList(),
                                                                                  LocalDate.parse("2019-04-30"),
                                                                                  LocalDate.parse("2022-04-30")))

        Assertions.assertThat(fullYtelse.size).isEqualTo(3)
        Assertions.assertThat(fullYtelse[0]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-04-30"), LocalDate.parse("2019-05-01"), 18166.toBigDecimal()))
        Assertions.assertThat(fullYtelse[1]).isEqualTo(Beløpsperiode(LocalDate.parse("2019-05-01"), LocalDate.parse("2020-05-01"), 18723.toBigDecimal()))
        Assertions.assertThat(fullYtelse[2]).isEqualTo(Beløpsperiode(LocalDate.parse("2020-05-01"), LocalDate.parse("2022-04-30"), 19003.toBigDecimal()))
    }
}