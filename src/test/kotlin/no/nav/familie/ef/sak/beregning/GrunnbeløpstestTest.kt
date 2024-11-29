package no.nav.familie.ef.sak.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrunnbeløpstestTest {
    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder går over flere grunnbeløpsperioder`() {
        val perioder = finnGrunnbeløpsPerioder(Månedsperiode(LocalDate.of(2001, 1, 1), LocalDate.of(2021, 8, 31)))
        assertThat(perioder.size).isEqualTo(22)
        perioder.forEachIndexed { index, beløpsperiode ->
            if (perioder.size > index + 1) {
                assertThat(beløpsperiode.periode.tom).isBefore(perioder[index + 1].periode.fom)
            }
        }
    }

    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder stanser på en g-endringsperiode`() {
        val perioder = finnGrunnbeløpsPerioder(Månedsperiode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 5, 1)))
        assertThat(perioder.size).isEqualTo(2)
        assertThat(perioder.first().periode.tomDato).isEqualTo(LocalDate.of(2021, 4, 30))
        assertThat(perioder.last().periode.fomDato).isEqualTo(LocalDate.of(2021, 5, 1))
        assertThat(perioder.last().periode.tomDato).isEqualTo(LocalDate.of(2021, 5, 31))
    }

    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder starter på en g-endringsperiode`() {
        val perioder = finnGrunnbeløpsPerioder(Månedsperiode(LocalDate.of(2021, 5, 1), LocalDate.of(2021, 8, 31)))
        assertThat(perioder.size).isEqualTo(1)
        assertThat(perioder.first().periode.fomDato).isEqualTo(LocalDate.of(2021, 5, 1))
        assertThat(perioder.first().periode.tomDato).isEqualTo(LocalDate.of(2021, 8, 31))
    }

    @Test
    internal fun `skal sette riktig startdato og sluttdato når beløpsperioder starter dagen før en en g-endringsperiode`() {
        val perioder = finnGrunnbeløpsPerioder(Månedsperiode(LocalDate.of(2021, 4, 30), LocalDate.of(2021, 8, 1)))
        assertThat(perioder.size).isEqualTo(2)
        assertThat(perioder.first().periode.fomDato).isEqualTo(LocalDate.of(2021, 4, 1))
        assertThat(perioder.first().periode.tomDato).isEqualTo(LocalDate.of(2021, 4, 30))
        assertThat(perioder.last().periode.fomDato).isEqualTo(LocalDate.of(2021, 5, 1))
        assertThat(perioder.last().periode.tomDato).isEqualTo(LocalDate.of(2021, 8, 31))
    }
}
