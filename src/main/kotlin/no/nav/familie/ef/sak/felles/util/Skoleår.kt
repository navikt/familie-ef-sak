package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.Month
import java.time.Year
import java.time.YearMonth

data class Skoleår(val år: Year) {

    constructor(periode: Månedsperiode) : this(utledSkoleår(periode.fom, periode.tom))

    // ty = Year formatted with 2 digits
    override fun toString(): String {
        return String.format("%ty/%ty", år, år.plusYears(1))
    }

    companion object {
        fun utledSkoleår(fra: YearMonth, til: YearMonth): Year {
            brukerfeilHvis(til < fra) {
                "Ugyldig skoleårsperiode: Tildato=$til må være etter eller lik fradato=$fra"
            }
            if (fra.month > Month.JUNE) {
                brukerfeilHvis(til.year == fra.year + 1 && til.month > Month.AUGUST) {
                    "Ugyldig skoleårsperiode: Når tildato er i neste år, så må måneden være før september"
                }
                brukerfeilHvis(til.year > fra.year + 1) {
                    "Ugyldig skoleårsperiode: Fradato og tildato må være i det samme skoleåret"
                }
                return Year.of(fra.year)
            } else {
                brukerfeilHvis(til.year != fra.year) {
                    "Ugyldig skoleårsperiode: Fradato før juli må ha tildato i det samme året"
                }
                brukerfeilHvis(til.month > Month.AUGUST) {
                    "Ugyldig skoleårsperiode: Fradato før juli må ha sluttmåned før september"
                }
                return Year.of(fra.year - 1)
            }
        }
    }
}
