package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import java.time.Month
import java.time.Year
import java.time.YearMonth

data class Skoleår(val år: Year) {

    override fun toString(): String {
        return String.format("%ty/%ty", år, år.plusYears(1))
    }
}

fun beregnOgValiderSkoleår(fra: YearMonth, til: YearMonth): Skoleår {
    brukerfeilHvis(til < fra) {
        "Tildato=$til må være etter eller lik fradato=$fra"
    }
    if (fra.month > Month.JUNE) {
        brukerfeilHvis(til.year == fra.year + 1 && til.month > Month.AUGUST) {
            "Når tildato er i neste år, så må måneden være før september"
        }
        brukerfeilHvis(til.year > fra.year + 1) {
            "Fradato og tildato må være i det samme skoleåret"
        }
        return Skoleår(Year.of(fra.year))
    } else {
        brukerfeilHvis(til.year != fra.year) {
            "Fradato før juli må ha tildato i det samme året"
        }
        brukerfeilHvis(til.month > Month.AUGUST) {
            "Fradato før juli må ha sluttmåned før september"
        }
        return Skoleår(Year.of(fra.year - 1))
    }
}
