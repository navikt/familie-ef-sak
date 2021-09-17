package no.nav.familie.ef.sak.felles.util

import java.time.LocalDate
import java.time.LocalDateTime

fun datoEllerIdag(localDate: LocalDate?): LocalDate = localDate ?: LocalDate.now()

fun min(dato1: LocalDateTime?, dato2: LocalDateTime?): LocalDateTime? =
        if (dato1 == null && dato2 == null) {
            null
        } else {
            minOf(dato1 ?: LocalDateTime.MAX, dato2 ?: LocalDateTime.MAX)
        }

fun LocalDate.isEqualOrBefore(other: LocalDate) = this == other || this.isBefore(other)
fun LocalDate.isEqualOrAfter(other: LocalDate) = this == other || this.isAfter(other)
