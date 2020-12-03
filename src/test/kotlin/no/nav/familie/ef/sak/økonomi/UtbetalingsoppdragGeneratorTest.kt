package no.nav.familie.ef.sak.økonomi

import org.junit.jupiter.api.Test

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
    fun `Har to perioder, legger til en tredje, endrer på den andre`() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/2_perioder_får_ny_periode_og_endring_i_andre_perioden.csv"))
    }

}
