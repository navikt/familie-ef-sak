package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.time.LocalDate

data class InternePerioder(
        val overgangsstønad: List<InternPeriode>,
        val barnetilsyn: List<InternPeriode>,
        val skolepenger: List<InternPeriode>,
)

/**
 * Brukes for å mappe interne ef-perioder og infotrygd perioder til ett felles format
 */
data class InternPeriode(
        val personIdent: String,
        val inntektsreduksjon: Int,
        val samordningsfradrag: Int,
        val beløp: Int, // netto_belop
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val opphørsdato: LocalDate?,
        val datakilde: PeriodeOvergangsstønad.Datakilde
) {

    fun erFullOvergangsstønad(): Boolean = this.inntektsreduksjon == 0 && this.samordningsfradrag == 0

}
