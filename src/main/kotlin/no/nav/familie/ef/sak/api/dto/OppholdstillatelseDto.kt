package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class OppholdstillatelseDto(val oppholdstillatelse: OppholdType, val fraDato: LocalDate?, val tilDato: LocalDate?)

enum class OppholdType {
    PERMANENT,
    MIDLERTIDIG,
    UKJENT
}