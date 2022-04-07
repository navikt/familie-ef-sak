package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil.satserForBarnetilsyn
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

internal class BeregningBarnetilsynUtilKtTest {

    @Test
    fun `Skal runde opp`() {
        assertThat(BigDecimal(9.2).roundUp().toInt()).isEqualTo(10)
        assertThat(BigDecimal(0).roundUp().toInt()).isEqualTo(0)
        assertThat(BigDecimal(1.0).roundUp().toInt()).isEqualTo(1)
        assertThat(BigDecimal(1).roundUp().toInt()).isEqualTo(1)
    }

    @Test
    fun `hente riktig sats for x barn`() {
        // 2022 = mapOf(1 to 4250, 2 to 5545, 3 to 6284)),
        val år2022 = YearMonth.of(2022, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2022)).isEqualTo(6284)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2022)).isEqualTo(6284)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2022)).isEqualTo(5545)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2022)).isEqualTo(4250)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2022)).isEqualTo(0)

        // 2020 =  mapOf(1 to 4053, 2 to 5289, 3 to 5993)
        val år2020 = YearMonth.of(2020, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2020)).isEqualTo(5993)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2020)).isEqualTo(5993)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2020)).isEqualTo(5289)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2020)).isEqualTo(4053)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2020)).isEqualTo(0)
    }

}