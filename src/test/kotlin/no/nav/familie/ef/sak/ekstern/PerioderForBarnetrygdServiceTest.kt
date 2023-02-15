package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InternPeriodeTestUtil.lagInternPeriode
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PerioderForBarnetrygdServiceTest {

    private val periodeService = mockk<PeriodeService>()

    private val service = PerioderForBarnetrygdService(periodeService)

    private val personIdent = "123"

    @Test
    internal fun `skal ikke ha med perioder som ikke har fullOvergangsstønad`() {
        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(lagInternPeriode(inntektsreduksjon = 1), lagInternPeriode(samordningsfradrag = 1))
        assertThat(service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder).isEmpty()
    }

    @Test
    internal fun `skal returnere perioder`() {
        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns listOf(lagInternPeriode(beløp = 1))
        assertThat(service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder).hasSize(1)
    }

    @Test
    internal fun `skal splitte opp overlappende perioder`() {
        val periode1 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 10, 1), stønadTom = LocalDate.of(2009, 1, 31))
        val periode2 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 12, 1), stønadTom = LocalDate.of(2009, 3, 31))
        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns listOf(
            periode1,
            periode2,
        )
        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder
        assertThat(perioder).hasSize(2)
        assertThat(perioder.first().fomDato).isEqualTo(periode2.stønadFom)
        assertThat(perioder.first().tomDato).isEqualTo(periode2.stønadTom)
        assertThat(perioder.last().fomDato).isEqualTo(periode1.stønadFom)
        assertThat(perioder.last().tomDato).isEqualTo(periode2.stønadFom.minusDays(1))
    }

    @Test
    internal fun `feilsituasjon prod - skal returnere en periode når den ene omsluttes av den andre`() {
        val periode1 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 12, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode2 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 10, 1), stønadTom = LocalDate.of(2009, 3, 31))
        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(periode1, periode2).sortedByDescending { it.stønadFom }
        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder
        assertThat(perioder).hasSize(1)
        assertThat(perioder.first().fomDato).isEqualTo(periode2.stønadFom)
        assertThat(perioder.first().tomDato).isEqualTo(periode2.stønadTom)
    }

    @Test
    internal fun `skal ikke endre på perioder som kommer fra EF`() {
        val periode1 =
            lagInternPeriode(
                beløp = 1,
                stønadFom = LocalDate.of(2008, 12, 1),
                stønadTom = LocalDate.of(2009, 3, 31),
                datakilde = PeriodeOvergangsstønad.Datakilde.EF,
            )
        val periode2 =
            lagInternPeriode(
                beløp = 1,
                stønadFom = LocalDate.of(2008, 10, 1),
                stønadTom = LocalDate.of(2009, 3, 31),
                datakilde = PeriodeOvergangsstønad.Datakilde.EF,
            )
        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(periode1, periode2).sortedByDescending { it.stønadFom }
        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder
        assertThat(perioder).hasSize(2)
        assertThat(perioder.first().fomDato).isEqualTo(periode1.stønadFom)
        assertThat(perioder.first().tomDato).isEqualTo(periode1.stønadTom)
        assertThat(perioder.last().fomDato).isEqualTo(periode2.stønadFom)
        assertThat(perioder.last().tomDato).isEqualTo(periode1.stønadTom)
    }

    @Test
    internal fun `skal fjerne perioder som omsluttes og splitte opp perioder som overlapper for Infotrygdperioder`() {
        val periode1 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 12, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode2 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 8, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode3 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 9, 1), stønadTom = LocalDate.of(2009, 1, 31))
        val periode4 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 1, 1), stønadTom = LocalDate.of(2009, 5, 31))
        val periode5 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 11, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode6 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2005, 1, 1), stønadTom = LocalDate.of(2005, 3, 31))
        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(periode1, periode2, periode3, periode4, periode5, periode6)
        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder
        assertThat(perioder).hasSize(2)

        assertThat(perioder[0].fomDato).isEqualTo(periode4.stønadFom)
        assertThat(perioder[0].tomDato).isEqualTo(periode4.stønadTom)
        assertThat(perioder[1].fomDato).isEqualTo(periode6.stønadFom)
        assertThat(perioder[1].tomDato).isEqualTo(periode6.stønadTom)
    }

    @Test
    internal fun `skal splitte og fjerne perioder riktig - også når det foreligger duplikate perioder som endres underveis i prosessen`() {
        val periode1 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 12, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode2 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 8, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode3 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 9, 1), stønadTom = LocalDate.of(2009, 1, 31))
        val periode4 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 10, 1), stønadTom = LocalDate.of(2009, 5, 31))
        val periode5 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 11, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode6 =
            lagInternPeriode(beløp = 1, stønadFom = LocalDate.of(2008, 9, 1), stønadTom = LocalDate.of(2009, 1, 31))

        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(periode1, periode2, periode3, periode4, periode5, periode6)
        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder
        assertThat(perioder).hasSize(2)

        assertThat(perioder[0].fomDato).isEqualTo(periode4.stønadFom)
        assertThat(perioder[0].tomDato).isEqualTo(periode4.stønadTom)
        assertThat(perioder[1].fomDato).isEqualTo(periode2.stønadFom)
        assertThat(perioder[1].tomDato).isEqualTo(periode4.stønadFom.minusDays(1))
    }

    @Test
    internal fun `periode omsluter annen periode`() {
        val periode1 = lagInternPeriode(stønadFom = LocalDate.of(2008, 8, 1), stønadTom = LocalDate.of(2009, 3, 31))
        val periode2 = lagInternPeriode(stønadFom = LocalDate.of(2008, 12, 1), stønadTom = LocalDate.of(2009, 3, 31))

        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(periode1, periode2)

        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].fomDato).isEqualTo(periode1.stønadFom)
        assertThat(perioder[0].tomDato).isEqualTo(periode1.stønadTom)
    }

    @Test
    internal fun `periode overlapper delvis`() {
        val periode1 = lagInternPeriode(stønadFom = LocalDate.of(2008, 8, 1), stønadTom = LocalDate.of(2009, 1, 31))
        val periode2 = lagInternPeriode(stønadFom = LocalDate.of(2008, 12, 1), stønadTom = LocalDate.of(2009, 3, 31))

        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(periode1, periode2)

        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder

        assertThat(perioder).hasSize(2)
        assertThat(perioder[0].fomDato).isEqualTo(periode2.stønadFom)
        assertThat(perioder[0].tomDato).isEqualTo(periode2.stønadTom)
        assertThat(perioder[1].fomDato).isEqualTo(periode1.stønadFom)
        assertThat(perioder[1].tomDato).isEqualTo(periode2.stønadFom.minusDays(1))
    }

    @Test
    internal fun `periode overlapper ikke`() {
        val periode1 = lagInternPeriode(stønadFom = LocalDate.of(2008, 8, 1), stønadTom = LocalDate.of(2009, 1, 31))
        val periode2 = lagInternPeriode(stønadFom = LocalDate.of(2009, 12, 1), stønadTom = LocalDate.of(2009, 12, 31))

        every { periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(any()) } returns
            listOf(periode1, periode2)

        val perioder = service.hentPerioderMedFullOvergangsstønad(PersonIdent(personIdent)).perioder

        assertThat(perioder).hasSize(2)
        assertThat(perioder[0].fomDato).isEqualTo(periode2.stønadFom)
        assertThat(perioder[0].tomDato).isEqualTo(periode2.stønadTom)
        assertThat(perioder[1].fomDato).isEqualTo(periode1.stønadFom)
        assertThat(perioder[1].tomDato).isEqualTo(periode1.stønadTom)
    }
}
