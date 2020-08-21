package no.nav.familie.ef.sak.util

import java.time.LocalDate

fun datoEllerIdag(localDate: LocalDate?): LocalDate = localDate ?: LocalDate.now()
