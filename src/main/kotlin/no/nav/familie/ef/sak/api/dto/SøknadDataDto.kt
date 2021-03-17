package no.nav.familie.ef.sak.api.dto

import java.time.LocalDateTime
import java.time.YearMonth

data class SøknadDataDto(val søknadsdato: LocalDateTime,
                         val søkerStønadFra: YearMonth? = null)
