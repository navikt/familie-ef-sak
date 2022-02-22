package no.nav.familie.ef.sak.felles.util

import java.time.LocalDate
import java.time.LocalDateTime
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

fun YearMonth.erPåfølgende(påfølgende: YearMonth): Boolean = this.plusMonths(1) == påfølgende
