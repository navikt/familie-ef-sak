package no.nav.familie.ef.sak.opplysninger.søknad

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class SøknadDatoerDto(
    val søknadsdato: LocalDateTime,
    val søkerStønadFra: YearMonth? = null,
    val datoPåbegyntSøknad: LocalDate? = null,
)
