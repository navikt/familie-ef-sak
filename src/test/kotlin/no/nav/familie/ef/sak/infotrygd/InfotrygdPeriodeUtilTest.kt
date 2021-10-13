package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeUtil.filtrerOgSorterPerioderFraInfotrygd
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class InfotrygdPeriodeUtilTest {

    @Test
    internal fun `skal sette tom-dato til opphør sitt dato hvis opphør sitt dato er før tom-dato`() {
        val stønadFom = LocalDate.now().minusDays(1)
        val stønadTom = LocalDate.now().plusDays(1)
        val opphørdato = LocalDate.now()
        val periode = lagInfotrygdPeriode(stønadFom = stønadFom,
                                          stønadTom = stønadTom,
                                          opphørdato = opphørdato)
        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode))
        assertThat(nyePerioder).hasSize(1)
        assertThat(nyePerioder[0].stønadFom).isEqualTo(stønadFom)
        assertThat(nyePerioder[0].stønadTom).isEqualTo(opphørdato)
        assertThat(nyePerioder[0].opphørsdato).isEqualTo(opphørdato)
    }

    @Test
    internal fun `skal ikke sette tom-dato til opphør sitt dato hvis opphør sitt dato er etter tom-dato`() {
        val stønadFom = LocalDate.now().minusDays(1)
        val stønadTom = LocalDate.now().plusDays(1)
        val opphørdato = LocalDate.now().plusDays(10)
        val periode = lagInfotrygdPeriode(stønadFom = stønadFom,
                                          stønadTom = stønadTom,
                                          opphørdato = opphørdato)
        val input = listOf(periode)
        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(input)
        assertThat(nyePerioder).isEqualTo(input)
    }

    @Test
    internal fun `skat sette tom til opphørdato -1 når opphør er første i måneden`() {
        val nowMåned = YearMonth.now()
        val fom = nowMåned.atDay(1)
        val tom = nowMåned.plusMonths(2).atEndOfMonth()
        val opphøsdato = fom.plusMonths(1)
        val periode1 = lagInfotrygdPeriode(stønadFom = fom,
                                           stønadTom = tom,
                                           opphørdato = opphøsdato)
        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode1))

        assertThat(nyePerioder).hasSize(1)
        assertThat(nyePerioder[0].stønadFom).isEqualTo(fom)
        assertThat(nyePerioder[0].stønadTom).isEqualTo(opphøsdato.minusDays(1))
        assertThat(nyePerioder[0].opphørsdato).isEqualTo(opphøsdato)
    }

    @Test
    internal fun `skal filtrere vekk perioden hvis opphør sitt dato er før fom-dato`() {
        val stønadFom = LocalDate.now().minusDays(1)
        val stønadTom = LocalDate.now().plusDays(1)
        val opphørdato = LocalDate.now().minusDays(10)
        val periode = lagInfotrygdPeriode(stønadFom = stønadFom,
                                          stønadTom = stønadTom,
                                          opphørdato = opphørdato)
        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode))
        assertThat(nyePerioder).isEmpty()
    }

    @Test
    internal fun `skal filtrere vekk perioden hvis tom-dato er før fom-dato`() {
        val stønadFom = LocalDate.now().minusDays(1)
        val stønadTom = LocalDate.now().minusDays(10)
        val opphørdato = null
        val periode = lagInfotrygdPeriode(stønadFom = stønadFom,
                                          stønadTom = stønadTom,
                                          opphørdato = opphørdato)
        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode))
        assertThat(nyePerioder).isEmpty()
    }

    @Test
    internal fun `skal ignorere duplikater`() {
        val periode = lagInfotrygdPeriode()

        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode, periode))

        assertThat(nyePerioder).hasSize(1)
        assertThat(nyePerioder[0]).isEqualTo(periode)
    }

    @Test
    internal fun `skal sortere perioder etter stønadId`() {
        val periode1 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 1, stønadFom = LocalDate.now())
        val periode2 = lagInfotrygdPeriode(stønadId = 2, vedtakId = 1, stønadFom = LocalDate.now())
        val periode3 = lagInfotrygdPeriode(stønadId = 3, vedtakId = 1, stønadFom = LocalDate.now())

        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode2, periode1, periode3))

        assertThat(nyePerioder).isEqualTo(listOf(periode3, periode2, periode1))
    }

    @Test
    internal fun `skal sortere perioder etter vedtakId`() {
        val periode1 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 1, stønadFom = LocalDate.now())
        val periode2 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 2, stønadFom = LocalDate.now())
        val periode3 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 3, stønadFom = LocalDate.now())

        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode2, periode1, periode3))

        assertThat(nyePerioder).isEqualTo(listOf(periode3, periode2, periode1))
    }

    @Test
    internal fun `skal sortere perioder etter stønadFom`() {
        val periode1 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 1, stønadFom = LocalDate.now().minusDays(3))
        val periode2 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 1, stønadFom = LocalDate.now().minusDays(2))
        val periode3 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 1, stønadFom = LocalDate.now().minusDays(1))

        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode2, periode1, periode3))

        assertThat(nyePerioder).isEqualTo(listOf(periode3, periode2, periode1))
    }

    @Test
    internal fun `skal sortere perioder etter stønadId, vedtakId og stønadTom`() {
        val periode1 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 1, stønadFom = LocalDate.now())
        val periode2 = lagInfotrygdPeriode(stønadId = 1, vedtakId = 2, stønadFom = LocalDate.now())
        val periode3 = lagInfotrygdPeriode(stønadId = 2, vedtakId = 1, stønadFom = LocalDate.now().minusDays(1))
        val periode4 = lagInfotrygdPeriode(stønadId = 2, vedtakId = 1, stønadFom = LocalDate.now())

        val nyePerioder = filtrerOgSorterPerioderFraInfotrygd(listOf(periode2, periode1, periode4, periode3))

        assertThat(nyePerioder).isEqualTo(listOf(periode4, periode3, periode2, periode1))
    }
}
