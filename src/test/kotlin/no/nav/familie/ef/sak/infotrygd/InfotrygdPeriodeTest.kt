package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class InfotrygdPeriodeTest {

    private val FOM = LocalDate.of(2021, 3, 1)
    private val TOM = LocalDate.of(2021, 5, 31)

    private val FØR_STARTDATO_FOM = LocalDate.of(2021, 1, 1)
    private val FØR_STARTDATO_TOM = LocalDate.of(2021, 1, 31)
    private val ETTER_SLUTDATO_FOM = LocalDate.of(2021, 7, 1)
    private val ETTER_SLUTTDATO_TOM = LocalDate.of(2021, 7, 31)
    private val MIDT_I_FOM = LocalDate.of(2021, 3, 1)
    private val MIDT_I_TOM = LocalDate.of(2021, 3, 31)

    private val startperiode = lagInfotrygdPeriode(FOM, TOM)


    @Test
    internal fun `erInfotrygdPeriodeOverlappende - ikke overlappende`() {
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(FØR_STARTDATO_FOM, FØR_STARTDATO_TOM)))
                .withFailMessage("Perioden starter og slutter før startperioden")
                .isFalse

        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(ETTER_SLUTDATO_FOM, ETTER_SLUTTDATO_TOM)))
                .withFailMessage("Perioden starter og slutter etter startperioden")
                .isFalse
    }

    @Test
    internal fun `erInfotrygdPeriodeOverlappende - overlappende startdato starter før`() {
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(FØR_STARTDATO_FOM, MIDT_I_TOM)))
                .withFailMessage("Perioden starter før og slutter midt i")
                .isTrue
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(FØR_STARTDATO_FOM, TOM)))
                .withFailMessage("Perioden starter før og slutter samtidig")
                .isTrue
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(FØR_STARTDATO_FOM, ETTER_SLUTTDATO_TOM)))
                .withFailMessage("Perioden starter før og slutter etter")
                .isTrue
    }

    @Test
    internal fun `erInfotrygdPeriodeOverlappende - overlappende startdato starter samtidig`() {
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(FOM, MIDT_I_TOM)))
                .withFailMessage("Perioden starter samtidig og slutter midt i")
                .isTrue
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(FOM, TOM)))
                .withFailMessage("Perioden starter og slutter samtidig")
                .isTrue
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(FOM, ETTER_SLUTTDATO_TOM)))
                .withFailMessage("Perioden starter og slutter etter")
                .isTrue
    }

    @Test
    internal fun `erInfotrygdPeriodeOverlappende - overlappende startdato starter midt i`() {
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(MIDT_I_FOM, MIDT_I_TOM)))
                .withFailMessage("Perioden starter midt i og slutter midt i")
                .isTrue
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(MIDT_I_FOM, TOM)))
                .withFailMessage("Perioden starter midt i og slutter samtidig")
                .isTrue
        assertThat(startperiode.erPeriodeOverlappende(lagInfotrygdPeriode(MIDT_I_FOM, ETTER_SLUTTDATO_TOM)))
                .withFailMessage("Perioden starter midt i og slutter etter")
                .isTrue
    }

    private fun lagInfotrygdPeriode(fom: LocalDate, tom: LocalDate) = InternPeriode(1, 1, 0, fom, tom, null, PeriodeOvergangsstønad.Datakilde.EF)
}