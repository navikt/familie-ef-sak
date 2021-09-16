package no.nav.familie.ef.sak.opplysninger.personopplysninger

import java.time.LocalDate

data class UtflyttingDto(val tilflyttingsland: String?, val dato: LocalDate?, val tilflyttingssted: String? = null)