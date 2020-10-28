package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.DataGenerator.dato
import no.nav.familie.ef.sak.økonomi.DataGenerator.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.DataGenerator.tilfeldigFødselsnummer
import no.nav.familie.ef.sak.økonomi.DataGenerator.tilfeldigTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.TestOppdragRunner
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingsoppdragGeneratorTest {

    @Test
    fun csvTest() {
        TestOppdragRunner.run(javaClass.getResource("/oppdrag/Sekvens1.csv"));
    }
}