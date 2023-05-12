package no.nav.familie.ef.sak.beregning

import org.junit.jupiter.api.Test
import java.time.YearMonth
import kotlin.test.assertTrue

internal class GrunnbeløpsperioderTest {

    @Test
    fun nyesteGrunnbeløp() {
        assertTrue { Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fom > YearMonth.of(2021, 5) }
    }

    @Test
    fun forrigeGrunnbeløp() {
        assertTrue { Grunnbeløpsperioder.forrigeGrunnbeløp.periode.fom < Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fom }
    }
}
