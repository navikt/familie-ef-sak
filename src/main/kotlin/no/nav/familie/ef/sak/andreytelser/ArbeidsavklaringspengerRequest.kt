package no.nav.familie.ef.sak.andreytelser

import java.time.LocalDate

data class ArbeidsavklaringspengerRequest(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val personidentifikator: String,
)
