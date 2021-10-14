package no.nav.familie.ef.sak.beregning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnbeløpstestTest {

    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder går over flere grunnbeløpsperioder`() {

        val perioder = finnGrunnbeløpsPerioder(LocalDate.of(2001, 1, 1), LocalDate.of(2021, 8, 31))
        assertThat(perioder.size).isEqualTo(22)
        perioder.forEachIndexed { index, beløpsperiode ->
            if (perioder.size > index + 1) {
                assertThat(beløpsperiode.periode.tildato).isBefore(perioder[index + 1].periode.fradato)
            }
        }
    }

    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder stanser på en g-endringsperiode`() {
        val perioder = finnGrunnbeløpsPerioder(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 5, 1))
        assertThat(perioder.size).isEqualTo(2)
        assertThat(perioder.first().periode.tildato).isEqualTo(LocalDate.of(2021, 4, 30))
        assertThat(perioder.last().periode.fradato).isEqualTo(LocalDate.of(2021, 5, 1))
        assertThat(perioder.last().periode.tildato).isEqualTo(LocalDate.of(2021, 5, 1))
    }


    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder starter på en g-endringsperiode`() {
        val perioder = finnGrunnbeløpsPerioder(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 8, 1))
        assertThat(perioder.size).isEqualTo(1)
        assertThat(perioder.first().periode.fradato).isEqualTo(LocalDate.of(2021, 5, 1))
        assertThat(perioder.first().periode.tildato).isEqualTo(LocalDate.of(2021, 8, 1))
    }

    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder starter dagen før en en g-endringsperiode`() {
        val perioder = finnGrunnbeløpsPerioder(LocalDate.of(2021, 4, 30), LocalDate.of(2021, 8, 1))
        assertThat(perioder.size).isEqualTo(2)
        assertThat(perioder.first().periode.fradato).isEqualTo(LocalDate.of(2021, 4, 30))
        assertThat(perioder.first().periode.tildato).isEqualTo(LocalDate.of(2021, 4, 30))
        assertThat(perioder.last().periode.fradato).isEqualTo(LocalDate.of(2021, 5, 1))
        assertThat(perioder.last().periode.tildato).isEqualTo(LocalDate.of(2021, 8, 1))
    }
}