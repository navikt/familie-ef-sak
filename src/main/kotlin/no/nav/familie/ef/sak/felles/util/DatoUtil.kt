package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.felles.util.DatoFormat.DATE_FORMAT_NORSK
import no.nav.familie.ef.sak.felles.util.DatoUtil.dagensDatoMedTid
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DatoFormat {

    val DATE_FORMAT_ISO_YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM")
    val YEAR_MONTH_FORMAT_NORSK = DateTimeFormatter.ofPattern("MM.yyyy")
    val DATE_FORMAT_NORSK = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val GOSYS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy' 'HH:mm")
}

object DatoUtil {

    fun dagensDatoMedTid(): LocalDateTime = LocalDateTime.now()
}

val YEAR_MONTH_MAX = YearMonth.from(LocalDate.MAX)

fun LocalDate.norskFormat() = this.format(DATE_FORMAT_NORSK)

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

fun LocalDate.er6MndEllerMer(): Boolean {
    return this.plusDays(183) <= LocalDate.now()
}

fun LocalDate.erEttÅrEllerMer(): Boolean {
    return this.plusYears(1) <= LocalDate.now()
}

fun LocalDate.er6MndEllerMerOgInnenforCutoff(numberOfDaysCutoff: Long): Boolean {
    return this.er6MndEllerMer() &&
        LocalDate.now() < this.plusDays(182).plusDays(numberOfDaysCutoff)
}

fun LocalDate.erEttÅrEllerMerOgInnenforCutoff(numberOfDaysCutoff: Long): Boolean {
    return erEttÅrEllerMer() &&
        LocalDate.now() <= this.plusYears(1).plusDays(numberOfDaysCutoff)
}

fun LocalDateTime.harGåttAntallTimer(timer: Int) =
    this.plusHours(timer.toLong()) < LocalDateTime.now()

fun dagensDatoMedTidNorskFormat(): String = dagensDatoMedTid().format(DatoFormat.GOSYS_DATE_TIME)
