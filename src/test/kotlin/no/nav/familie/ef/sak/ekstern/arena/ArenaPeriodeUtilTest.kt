package no.nav.familie.ef.sak.ekstern.arena

import no.nav.familie.ef.sak.ekstern.stønadsperiode.util.ArenaPeriodeUtil.slåSammenPerioderFraEfOgInfotrygd
import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.ef.sak.infotrygd.InternePerioder
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.time.YearMonth.of

internal class ArenaPeriodeUtilTest {
    @Test
    internal fun `sammenhengende periode fra ef og infotrygd slås sammen tvers stønadstyper`() {
        val request = request(of(2022, 1), of(2022, 1))
        val perioder =
            internePerioder(
                listOf(periode(of(2021, 1), of(2022, 1))),
                listOf(periode(of(2022, 2), of(2023, 1))),
            )

        val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
        assertThat(resultat).containsExactly(lagResultatPeriode(of(2021, 1), of(2023, 1)))
    }

    @Test
    internal fun `ikke sammenhengende periode tvers stønadstyper fra ef og infotrygd returneres oppdelte`() {
        val request = request(of(2022, 1), of(2022, 4))
        val perioder =
            internePerioder(
                listOf(periode(of(2021, 1), of(2022, 1))),
                listOf(periode(of(2022, 3), of(2023, 1))),
            )

        val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
        assertThat(resultat).containsExactly(
            lagResultatPeriode(of(2021, 1), of(2022, 1)),
            lagResultatPeriode(of(2022, 3), of(2023, 1)),
        )
    }

    @Test
    internal fun `ikke sammenhengende periode av en type stønad fra ef og infotrygd returneres oppdelte`() {
        val request = request(of(2022, 1), of(2022, 4))
        val perioder =
            internePerioder(
                listOf(
                    periode(of(2021, 1), of(2022, 1)),
                    periode(of(2022, 3), of(2023, 1)),
                ),
            )

        val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
        assertThat(resultat).containsExactly(
            lagResultatPeriode(of(2021, 1), of(2022, 1)),
            lagResultatPeriode(of(2022, 3), of(2023, 1)),
        )
    }

    @Test
    internal fun `request overlapper kun en av periodene`() {
        val request = request(of(2022, 1), of(2022, 1))
        val perioder =
            internePerioder(
                listOf(
                    periode(of(2021, 1), of(2022, 1)),
                    periode(of(2022, 3), of(2023, 1)),
                ),
            )

        val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
        assertThat(resultat).containsExactly(lagResultatPeriode(of(2021, 1), of(2022, 1)))
    }

    @Test
    internal fun `overlappende periode fra ef og infotrygd slås sammen tvers stønadstyper`() {
        val request = request(of(2022, 1), of(2022, 1))
        val perioder =
            internePerioder(
                listOf(periode(of(2021, 11), of(2022, 1))),
                listOf(periode(of(2021, 1), of(2023, 1))),
            )

        val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
        assertThat(resultat).containsExactly(lagResultatPeriode(of(2021, 1), of(2023, 1)))
    }

    @Nested
    inner class `Overlappende datoer` {
        private val fom = of(2021, 3)
        private val tom = of(2021, 5)
        private val perioder = internePerioder(listOf(periode(fom, tom)))
        private val expected = lagResultatPeriode(fom, tom)

        @Test
        internal fun `overlapper startdato`() {
            val request = request(fom.minusMonths(1), tom.minusMonths(1))
            val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
            assertThat(resultat).containsExactly(expected)
        }

        @Test
        internal fun `overlapper sluttdato`() {
            val request = request(fom.plusMonths(1), tom.plusMonths(1))
            val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
            assertThat(resultat).containsExactly(expected)
        }

        @Test
        internal fun `delmengde av periode`() {
            val request = request(fom.plusMonths(1), tom.minusMonths(1))
            val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
            assertThat(resultat).containsExactly(expected)
        }

        @Test
        internal fun `sammensluter periode`() {
            val request = request(fom.minusMonths(1), tom.plusMonths(1))
            val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
            assertThat(resultat).containsExactly(expected)
        }

        @Test
        internal fun `før periode`() {
            val request = request(fom.minusMonths(1), fom.minusMonths(1))
            val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
            assertThat(resultat).isEmpty()
        }

        @Test
        internal fun `etter periode`() {
            val request = request(tom.plusMonths(1), tom.plusMonths(1))
            val resultat = slåSammenPerioderFraEfOgInfotrygd(request, perioder)
            assertThat(resultat).isEmpty()
        }
    }
}

private val ident = "01234567890"

private fun request(
    fom: YearMonth,
    tom: YearMonth,
) = EksternePerioderRequest(ident, fom.atDay(1), tom.atEndOfMonth())

private fun periode(
    fom: YearMonth,
    tom: YearMonth,
) = InternPeriode(
    personIdent = ident,
    inntektsreduksjon = 0,
    samordningsfradrag = 0,
    utgifterBarnetilsyn = 0,
    månedsbeløp = 0,
    engangsbeløp = 0,
    stønadFom = fom.atDay(1),
    stønadTom = tom.atEndOfMonth(),
    opphørsdato = null,
    datakilde = Datakilde.EF,
)

private fun internePerioder(
    overgangsstønad: List<InternPeriode> = emptyList(),
    barnetilsyn: List<InternPeriode> = emptyList(),
    skolepenger: List<InternPeriode> = emptyList(),
) = InternePerioder(overgangsstønad, barnetilsyn, skolepenger)

private fun lagResultatPeriode(
    fom: YearMonth,
    tom: YearMonth,
) = EksternPeriode(
    personIdent = ident,
    fomDato = fom.atDay(1),
    tomDato = tom.atEndOfMonth(),
    datakilde = Datakilde.EF,
)
