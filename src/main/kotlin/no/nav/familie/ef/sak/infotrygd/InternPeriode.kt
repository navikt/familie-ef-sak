package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.felles.ef.Datakilde
import java.time.LocalDate

data class InternePerioder(
    val overgangsstønad: List<InternPeriode>,
    val barnetilsyn: List<InternPeriode>,
    val skolepenger: List<InternPeriode>
)

/**
 * Holder for å sette startdato fra tilkjent ytelse sammen med internperioder
 */
data class EfInternPerioder(
    val startdato: LocalDate,
    val internperioder: List<InternPeriode>
)

/**
 * Brukes for å mappe interne ef-perioder og infotrygd perioder til ett felles format
 */
data class InternPeriode(
    val personIdent: String,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int,
    val utgifterBarnetilsyn: Int,
    val månedsbeløp: Int,
    val engangsbeløp: Int,
    val stønadFom: LocalDate,
    val stønadTom: LocalDate,
    val opphørsdato: LocalDate?,
    val datakilde: Datakilde
) {

    fun erFullOvergangsstønad(): Boolean = this.inntektsreduksjon == 0 && this.samordningsfradrag == 0
}
