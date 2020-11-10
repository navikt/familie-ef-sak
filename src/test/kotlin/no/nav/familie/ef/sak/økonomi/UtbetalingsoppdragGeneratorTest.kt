package no.nav.familie.ef.sak.økonomi

import org.junit.jupiter.api.Test

internal class UtbetalingsoppdragGeneratorTest {

    @Test
    fun csvTest() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/Sekvens1.csv"));
    }
}