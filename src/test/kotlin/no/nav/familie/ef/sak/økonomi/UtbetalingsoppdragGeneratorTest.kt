package no.nav.familie.ef.sak.økonomi

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.opentest4j.ValueWrapper

internal class UtbetalingsoppdragGeneratorTest {

    @Test
    fun csvTest() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/Sekvens1.csv"))
    }

    @Test
    fun `Har en periode og får en endring mitt i perioden`() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/1_periode_får_en_endring_i_perioden.csv"))
    }

    @Test
    fun `Har to perioder og får en endring første perioden`() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/2_perioder_får_en_endring_i_første_perioden.csv"))
    }

    @Test
    fun `Har tre perioder og får en endring første perioden`() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/3_perioder_får_en_endring_i_første_perioden.csv"))
    }

    @Test
    fun `Har tre perioder og får en endring andre perioden`() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/3_perioder_får_en_endring_i_andre_perioden.csv"))
    }

    @Test
    fun `Har en periode og får ett opphør`() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/1_periode_får_ett_opphør.csv"))
    }

    @Test
    fun `Har 2 perioder og får en endring på andre perioden men har feil behandlingId i testen`() {
        val catchThrowable = catchThrowable {
            TestOppdragRunner.run(javaClass.getResource("/oppdrag/2_periode_får_en_endring_i_andre_perioden_feiler_pga_feil_behandling_id.csv"))
        }
        assertThat(catchThrowable)
                .hasMessageContaining("Feiler for gruppe med indeks 1 ==> ")
                .isInstanceOf(AssertionFailedError::class.java)

        assertExpectedOgActualErLikeUtenomFeltSomFeiler(catchThrowable, "ursprungsbehandlingId")
    }

    @Test
    fun `Har 2 perioder og får en endring på andre perioden men har feil periodeId i testen`() {
        val catchThrowable = catchThrowable {
            TestOppdragRunner.run(javaClass.getResource("/oppdrag/2_periode_får_en_endring_i_andre_perioden_feiler_pga_feil_periode_id.csv"))
        }
        assertThat(catchThrowable)
                .hasMessageContaining("Feiler for gruppe med indeks 1 ==> ")
                .isInstanceOf(AssertionFailedError::class.java)

        assertExpectedOgActualErLikeUtenomFeltSomFeiler(catchThrowable, "periodeId")
    }

    private fun assertExpectedOgActualErLikeUtenomFeltSomFeiler(catchThrowable: Throwable?,
                                                                feltSomSkalFiltreres: String) {
        val assertionFailedError = catchThrowable as AssertionFailedError
        val actual = filterAwayBehandlingId(assertionFailedError.actual, feltSomSkalFiltreres)
        val expected = filterAwayBehandlingId(assertionFailedError.expected, feltSomSkalFiltreres)
        assertThat(actual).isEqualTo(expected)
    }

    private fun filterAwayBehandlingId(valueWrapper: ValueWrapper, feltSomSkalFiltreres: String) =
            valueWrapper.stringRepresentation
                    .split("\n")
                    .filterNot { it.contains(feltSomSkalFiltreres) }
                    .joinToString("\n")

}
