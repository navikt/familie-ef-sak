package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class PersonMinimumDto(val navn: String, val fødselsdato: LocalDate?, val personIdent: String?)