package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DatoFormat {

    val DATE_FORMAT_ISO_YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM")
}

fun datoEllerIdag(localDate: LocalDate?): LocalDate = localDate ?: LocalDate.now()

fun min(first: LocalDateTime?, second: LocalDateTime?): LocalDateTime? {
    return when {
        first == null -> second
        second == null -> first
        else -> minOf(first, second)
    }
}

fun min(first: LocalDate?, second: LocalDate?): LocalDate? {
    return when {
        first == null -> second
        second == null -> first
        else -> minOf(first, second)
    }
}

fun LocalDate.isEqualOrBefore(other: LocalDate) = this == other || this.isBefore(other)
fun LocalDate.isEqualOrAfter(other: LocalDate) = this == other || this.isAfter(other)

fun LocalDate.harPåfølgendeMåned(påfølgende: LocalDate): Boolean =
    YearMonth.from(this).erPåfølgende(YearMonth.from(påfølgende))

fun YearMonth.erPåfølgende(påfølgende: YearMonth): Boolean = this.plusMonths(1) == påfølgende

fun YearMonth.erSammeMåned(sammeMåned: YearMonth): Boolean = this == sammeMåned

fun skoleår(fra: YearMonth, til: YearMonth): Skoleår {
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

data class Skoleår(val år: Year) {

    override fun toString(): String {
        return String.format("%ty/%ty", år, år.plusYears(1))
    }
}