package no.nav.familie.ef.sak.opplysninger.personopplysninger.dto

import java.time.LocalDate

data class InnflyttingDto(val fraflyttingsland: String?, val dato: LocalDate?, val fraflyttingssted: String? = null)