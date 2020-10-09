package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Selvstendig(val firmanavn: String,
                       val organisasjonsnummer: String,
                       val etableringsdato: LocalDate,
                       val arbeidsmengde: Int? = null,
                       val hvordanSerArbeidsukenUt: String)

/**
 * Arbeidsmengde skal ikke fylles ut av Barnetilsyn
 */
