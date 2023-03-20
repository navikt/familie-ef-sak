package no.nav.familie.ef.sak.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class InntektsperiodeTest {
    val inntektsperiode =
        Inntektsperiode(
            periode = Månedsperiode(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 4, 30)
            ),
            inntekt = BigDecimal.ZERO,
            samordningsfradrag = BigDecimal.ZERO
        )

    @Test
    internal fun `skal multiplisere dagsats med 260`() {
        val dagsats = 100.toBigDecimal()
        val nyInntektsperiode = inntektsperiode.copy(dagsats = dagsats)

        assertThat(nyInntektsperiode.totalinntekt()).isEqualTo(dagsats.multiply(BigDecimal(260)))
    }

    @Test
    internal fun `skal multiplisere månedsinntekt med 12`() {
        val månedsinntekt = 100_000.toBigDecimal()
        val nyInntektsperiode = inntektsperiode.copy(månedsinntekt = månedsinntekt)

        assertThat(nyInntektsperiode.totalinntekt()).isEqualTo(månedsinntekt.multiply(BigDecimal(12)))
    }

    @Test
    internal fun `skal returnere årsinntekt direkte som inntekt om det er eneste type`() {
        val årsinntekt = 500_000.toBigDecimal()
        val nyInntektsperiode = inntektsperiode.copy(inntekt = årsinntekt)

        assertThat(nyInntektsperiode.totalinntekt()).isEqualTo(årsinntekt)
    }
}
