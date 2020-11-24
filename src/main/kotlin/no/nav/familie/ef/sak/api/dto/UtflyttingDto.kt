package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class UtflyttingDto(val tilflyttingsland: String?, val dato: LocalDate?, val tilflyttingssted: String? = null)