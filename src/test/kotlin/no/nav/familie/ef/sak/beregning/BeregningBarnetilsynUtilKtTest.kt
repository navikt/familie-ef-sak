package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil.satserForBarnetilsyn
import no.nav.familie.ef.sak.beregning.barnetilsyn.hentSatsFor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class BeregningBarnetilsynUtilKtTest {

    @Test
    fun `hente riktig sats for 0, 1,2,3,4 barn i år 20 og 22`() {
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