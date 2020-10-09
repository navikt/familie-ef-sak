package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class PersonMinimum(val navn: String,
                         val fødselsnummer: Fødselsnummer? = null,
                         val fødselsdato: LocalDate? = null,
                         val land: String? = null)
