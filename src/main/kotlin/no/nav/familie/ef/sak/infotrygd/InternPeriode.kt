package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.time.LocalDate

/**
 * Brukes for å mappe interne ef-perioder og infotrygd perioder til ett felles format
 */
data class InternPeriode(
        val personIdent: String,
        val inntektsreduksjon: Int,
        val samordningsfradrag: Int,
        val beløp: Int, // netto_belop
        var stønadFom: LocalDate,
        var stønadTom: LocalDate,
        val opphørsdato: LocalDate?,
        val datakilde: PeriodeOvergangsstønad.Datakilde
) {

    private fun erDatoInnenforPeriode(dato: LocalDate): Boolean {
        return dato.isEqualOrBefore(stønadTom) && dato.isEqualOrAfter(stønadFom)
    }

    fun erPeriodeOverlappende(periode: InternPeriode): Boolean {
        return (erDatoInnenforPeriode(periode.stønadFom) || erDatoInnenforPeriode(periode.stønadTom))
               || omslutter(periode)
    }

    private fun omslutter(periode: InternPeriode) =
            periode.stønadFom.isBefore(stønadFom) && periode.stønadTom.isAfter(stønadTom)

    fun erFullOvergangsstønad(): Boolean = this.inntektsreduksjon == 0 && this.samordningsfradrag == 0

}
