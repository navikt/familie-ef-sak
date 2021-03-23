package no.nav.familie.ef.sak.api.dto

import java.time.LocalDateTime
import java.time.YearMonth

data class SøknadDatoerDto(val søknadsdato: LocalDateTime,
                           val søkerStønadFra: YearMonth? = null)
