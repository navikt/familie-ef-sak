package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil.satserForBarnetilsyn
import no.nav.familie.ef.sak.beregning.barnetilsyn.hentSatsFor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class BeregningBarnetilsynUtilKtTest {
    @Test
    fun `Sjekk at det er lagt inn ny makssats for dette året`() {
        val åretsMakssats = satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = YearMonth.now())
        val fjoråretsMakssats = satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = YearMonth.now().minusYears(1))
        assertThat(åretsMakssats).isNotEqualTo(fjoråretsMakssats)
    }

    @Test
    fun `hente riktig sats for barn for år 2025`() {
        val januar2025 = YearMonth.of(2025, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = januar2025)).isEqualTo(7081)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = januar2025)).isEqualTo(7081)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = januar2025)).isEqualTo(6248)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = januar2025)).isEqualTo(4790)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = januar2025)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2024`() {
        val januar2024 = YearMonth.of(2024, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = januar2024)).isEqualTo(6875)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = januar2024)).isEqualTo(6875)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = januar2024)).isEqualTo(6066)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = januar2024)).isEqualTo(4650)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = januar2024)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2023`() {
        val juli2023 = YearMonth.of(2023, 7)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = juli2023)).isEqualTo(6623)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = juli2023)).isEqualTo(6623)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = juli2023)).isEqualTo(5844)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = juli2023)).isEqualTo(4480)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = juli2023)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2023 første halvdel`() {
        val januar2023 = YearMonth.of(2023, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = januar2023)).isEqualTo(6460)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = januar2023)).isEqualTo(6460)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = januar2023)).isEqualTo(5700)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = januar2023)).isEqualTo(4369)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = januar2023)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2022`() {
        val år2022 = YearMonth.of(2022, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2022)).isEqualTo(6284)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2022)).isEqualTo(6284)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2022)).isEqualTo(5545)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2022)).isEqualTo(4250)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2022)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2021`() {
        val år2021 = YearMonth.of(2021, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2021)).isEqualTo(6203)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2021)).isEqualTo(6203)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2021)).isEqualTo(5474)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2021)).isEqualTo(4195)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2021)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2020`() {
        val år2020 = YearMonth.of(2020, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2020)).isEqualTo(5993)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2020)).isEqualTo(5993)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2020)).isEqualTo(5289)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2020)).isEqualTo(4053)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2020)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2019`() {
        val år2019 = YearMonth.of(2019, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2019)).isEqualTo(5881)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2019)).isEqualTo(5881)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2019)).isEqualTo(5190)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2019)).isEqualTo(3977)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2019)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2016, 2017, 2018`() {
        listOf(2018, 2017, 2016).forEach {
            val år = YearMonth.of(it, 1)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år)).isEqualTo(5749)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år)).isEqualTo(5749)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år)).isEqualTo(5074)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år)).isEqualTo(3888)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år)).isEqualTo(0)
        }
    }
}
