package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Utenlandsopphold(val fradato: LocalDate,
                            val tildato: LocalDate,
                            val årsakUtenlandsopphold: String)
